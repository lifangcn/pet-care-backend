package pvt.mktech.petcare.knowledge.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.knowledge.entity.KnowledgeDocument;
import pvt.mktech.petcare.knowledge.entity.codelist.ProcessingStatusOfKnowledgeDocument;
import pvt.mktech.petcare.knowledge.mapper.KnowledgeDocumentMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentServiceImplTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final KeywordMetadataEnricher enricher = mock(KeywordMetadataEnricher.class);
    private final KnowledgeDocumentMapper mapper = mock(KnowledgeDocumentMapper.class);
    private final OssTemplate ossTemplate = mock(OssTemplate.class);
    private final KnowledgeDocumentServiceImpl service = new KnowledgeDocumentServiceImpl(ossTemplate, vectorStore, enricher);

    KnowledgeDocumentServiceImplTest() {
        ReflectionTestUtils.setField(service, "mapper", mapper);
        when(enricher.apply(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.update(any(KnowledgeDocument.class), anyBoolean())).thenReturn(1);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteMarksEntityDeleted() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.delete();
        assertThat(document.getIsDeleted()).isTrue();
    }

    @Test
    void deleteUpdatesDatabaseCleansVectorsAndDefersOssUntilCommit() throws Exception {
        KnowledgeDocument document = document(11L, "oss://document");
        when(mapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(document);
        TransactionSynchronizationManager.initSynchronization();

        service.deleteDocument(11L);

        verify(mapper).update(eq(document), eq(true));
        verify(mapper, never()).insert(any());
        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(ossTemplate, never()).deleteFile(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(ossTemplate).deleteFile("oss://document");
    }

    @Test
    void deleteDoesNotReportSuccessOrDeleteOssWhenVectorCleanupFails() throws Exception {
        KnowledgeDocument document = document(12L, "oss://document");
        when(mapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(document);
        doThrow(new IllegalStateException("vector down")).when(vectorStore).delete(any(Filter.Expression.class));

        assertThatThrownBy(() -> service.deleteDocument(12L)).isInstanceOf(IllegalStateException.class);

        verify(mapper).update(eq(document), eq(true));
        verify(ossTemplate, never()).deleteFile(any());
    }

    @Test
    void processingClearsExistingVectorsBeforeAddingStableChunks() {
        KnowledgeDocument stored = document(77L, "oss://ignored");
        when(mapper.selectOneById(77L)).thenReturn(stored);
        MockMultipartFile file = markdown("care-vaccination.md", "# Vaccination\nPuppy vaccination schedule.");

        service.processDocumentToVectorStoreAsync(77L, file);

        ArgumentCaptor<List<Document>> batches = ArgumentCaptor.forClass(List.class);
        var order = org.mockito.Mockito.inOrder(vectorStore);
        order.verify(vectorStore).delete(any(Filter.Expression.class));
        order.verify(vectorStore).add(batches.capture());
        Document chunk = batches.getValue().getFirst();
        assertThat(chunk.getId()).isEqualTo(UUID.nameUUIDFromBytes("77:0".getBytes(StandardCharsets.UTF_8)).toString());
        assertThat(chunk.getMetadata()).containsEntry("document_id", "77").containsEntry("chunk_index", 0)
                .containsEntry("filename", "care-vaccination.md");
    }

    @Test
    void failedSecondBatchCleansAllVectorsByDocumentFilterAndMarksFailure() {
        KnowledgeDocument stored = document(78L, "oss://ignored");
        when(mapper.selectOneById(78L)).thenReturn(stored);
        AtomicInteger adds = new AtomicInteger();
        doAnswer(invocation -> {
            if (adds.incrementAndGet() == 2) throw new IllegalStateException("vector unavailable");
            return null;
        }).when(vectorStore).add(any());

        service.processDocumentToVectorStoreAsync(78L, markdown("care-rollback.md", "# Care\n" + "vaccination nutrition training ".repeat(5000)));

        verify(vectorStore, times(2)).add(any());
        verify(vectorStore, times(2)).delete(any(Filter.Expression.class));
        assertThat(stored.getProcessingStatus()).isEqualTo(ProcessingStatusOfKnowledgeDocument.FAILED);
        assertThat(stored.getProcessingError()).contains("vector unavailable");
    }

    @Test
    void reindexReadsOssRebuildsVectorsAndCompletesDocument() throws Exception {
        KnowledgeDocument document = document(79L, "oss://reindex");
        when(mapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(document);
        when(mapper.selectOneById(79L)).thenReturn(document);
        when(ossTemplate.getInputStreamByUrl("oss://reindex")).thenReturn(new ByteArrayInputStream("# Care\nRebuilt from OSS.".getBytes(StandardCharsets.UTF_8)));

        service.reindexDocument(79L);

        verify(ossTemplate).getInputStreamByUrl("oss://reindex");
        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(vectorStore).add(any());
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatusOfKnowledgeDocument.COMPLETED);
    }

    @Test
    void reindexCleanupFailureMarksFailedAndThrowsBusinessException() throws Exception {
        KnowledgeDocument document = document(80L, "oss://reindex");
        when(mapper.selectOneByQuery(any(QueryWrapper.class))).thenReturn(document);
        when(mapper.selectOneById(80L)).thenReturn(document);
        when(ossTemplate.getInputStreamByUrl("oss://reindex")).thenReturn(new ByteArrayInputStream("# Care".getBytes(StandardCharsets.UTF_8)));
        doThrow(new IllegalStateException("cleanup down")).when(vectorStore).delete(any(Filter.Expression.class));

        assertThatThrownBy(() -> service.reindexDocument(80L)).isInstanceOf(BusinessException.class);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatusOfKnowledgeDocument.FAILED);
        assertThat(document.getProcessingError()).contains("cleanup down");
    }

    private KnowledgeDocument document(Long id, String fileUrl) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setName("care-vaccination.md");
        document.setFileUrl(fileUrl);
        return document;
    }

    private MockMultipartFile markdown(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/markdown", content.getBytes(StandardCharsets.UTF_8));
    }
}
