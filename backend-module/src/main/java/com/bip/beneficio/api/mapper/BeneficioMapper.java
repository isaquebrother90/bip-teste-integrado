package com.bip.beneficio.api.mapper;

import com.bip.beneficio.api.dto.BeneficioCreateDTO;
import com.bip.beneficio.api.dto.BeneficioDTO;
import com.bip.beneficio.api.dto.BeneficioMetadataDTO;
import com.bip.beneficio.api.dto.BeneficioUpdateDTO;
import com.bip.beneficio.domain.entity.Beneficio;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BeneficioMapper {

    BeneficioDTO toDTO(Beneficio entity);

    List<BeneficioDTO> toDTOList(List<Beneficio> entities);

    /** Mapeamento para metadados — omite o campo valor intencionalmente. */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nome", source = "nome")
    @Mapping(target = "descricao", source = "descricao")
    @Mapping(target = "ativo", source = "ativo")
    BeneficioMetadataDTO toMetadataDTO(Beneficio entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Beneficio toEntity(BeneficioCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    void updateEntityFromDTO(BeneficioUpdateDTO dto, @MappingTarget Beneficio entity);
}
