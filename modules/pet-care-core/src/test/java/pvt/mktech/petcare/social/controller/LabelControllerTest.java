package pvt.mktech.petcare.social.controller;

import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.social.entity.Label;
import pvt.mktech.petcare.social.entity.codelist.TypeOfLabel;
import pvt.mktech.petcare.social.service.LabelService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelControllerTest {

    private final LabelService labelService = mock(LabelService.class);
    private final LabelController controller = new LabelController(labelService);

    @Test
    void getLabelListPassesEnumTypeWhenProvided() {
        List<Label> labels = List.of(new Label());
        when(labelService.listLabelByType(TypeOfLabel.GENERAL)).thenReturn(labels);

        Result<List<Label>> result = controller.getLabelList(TypeOfLabel.GENERAL);

        assertThat(result.getData()).isEqualTo(labels);
        verify(labelService).listLabelByType(TypeOfLabel.GENERAL);
    }

    @Test
    void getLabelListPassesNullWhenTypeOmitted() {
        List<Label> labels = List.of(new Label(), new Label());
        when(labelService.listLabelByType(null)).thenReturn(labels);

        Result<List<Label>> result = controller.getLabelList(null);

        assertThat(result.getData()).isEqualTo(labels);
        verify(labelService).listLabelByType(null);
    }
}
