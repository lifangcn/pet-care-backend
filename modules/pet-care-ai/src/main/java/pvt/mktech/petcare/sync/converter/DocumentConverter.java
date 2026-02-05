package pvt.mktech.petcare.sync.converter;

import pvt.mktech.petcare.sync.dto.event.CdcData;

/**
 * {@code @description}: 文档转换器接口
 * <p>将CDC数据转换为ES文档</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public interface DocumentConverter<T extends CdcData, R> {

    /**
     * 将CDC数据转换为ES文档
     *
     * @param cdcData CDC数据
     * @return ES文档
     */
    R convert(T cdcData);
}
