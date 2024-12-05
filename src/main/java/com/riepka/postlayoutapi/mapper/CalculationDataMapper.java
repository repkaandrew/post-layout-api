package com.riepka.postlayoutapi.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.riepka.postlayoutapi.entity.CalculationData;
import com.riepka.postlayoutapi.entity.LayoutCalculationInput;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface CalculationDataMapper {

  CalculationData toCalculationData(LayoutCalculationInput input);
}
