package pvt.mktech.petcare.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import pvt.mktech.petcare.social.entity.Label;
import pvt.mktech.petcare.social.entity.codelist.TypeOfLabel;
import pvt.mktech.petcare.social.mapper.LabelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelServiceImplTest {

    private final LabelMapper labelMapper = mock(LabelMapper.class);
    private final LabelServiceImpl labelService = new LabelServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(labelService, "mapper", labelMapper);
    }

    @Test
    void listLabelByTypeUsesStringEnumForGeneral() {
        when(labelMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(new Label()));

        labelService.listLabelByType(TypeOfLabel.GENERAL);

        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(labelMapper).selectListByQuery(wrapperCaptor.capture());
        QueryWrapper wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.toSQL())
                .containsIgnoringCase("type")
                .containsIgnoringCase("=")
                .containsIgnoringCase("'GENERAL'");
    }

    @Test
    void listLabelByTypeOmitsFilterWhenNull() {
        when(labelMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(new Label(), new Label()));

        List<Label> result = labelService.listLabelByType(null);

        assertThat(result).hasSize(2);
        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(labelMapper).selectListByQuery(wrapperCaptor.capture());
        QueryWrapper wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.toSQL()).doesNotContainIgnoringCase("GENERAL");
    }
}
