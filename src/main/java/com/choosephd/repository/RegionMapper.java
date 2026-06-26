package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.Region;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地区字典 mapper — 国家所属大洲（亚洲/欧洲/北美洲等）。
 *
 * <p>继承 {@code BaseMapper<Region>}。地区表只读，{@code UniversityService}
 * 硬编码 CONTINENT_COUNTRIES 静态 Map 优先用，本 mapper 仅备查。
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {
}
