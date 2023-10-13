package com.riepka.postlayoutapi.mapper;

import com.riepka.postlayoutapi.entity.CalculationData;
import com.riepka.postlayoutapi.entity.LayoutCalculationInput;
import com.riepka.postlayoutapi.entity.Obstruction;
import com.riepka.postlayoutapi.entity.ObstructionInput;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CalculationDataMapper {

  CalculationDataMapper INSTANCE = Mappers.getMapper(CalculationDataMapper.class);

  CalculationData toCalculationData(LayoutCalculationInput input);

  Obstruction toObstruction(ObstructionInput input);
}
