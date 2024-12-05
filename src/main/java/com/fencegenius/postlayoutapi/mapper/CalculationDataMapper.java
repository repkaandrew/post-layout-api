package com.fencegenius.postlayoutapi.mapper;

import com.fencegenius.postlayoutapi.entity.CalculationData;
import com.fencegenius.postlayoutapi.entity.LayoutCalculationInput;
import com.fencegenius.postlayoutapi.entity.Obstruction;
import com.fencegenius.postlayoutapi.entity.ObstructionInput;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CalculationDataMapper {

  CalculationDataMapper INSTANCE = Mappers.getMapper(CalculationDataMapper.class);

  CalculationData toCalculationData(LayoutCalculationInput input);

  Obstruction toObstruction(ObstructionInput input);
}
