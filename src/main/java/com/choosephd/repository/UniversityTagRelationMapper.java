package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.UniversityTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院校-标签多对多关联 mapper — 关联表 CRUD。
 *
 * <p>继承 {@code BaseMapper<UniversityTagRelation>}。所有操作走 admin
 * {@link com.choosephd.service.UniversityTagService#setUniversityTags}，
 * 本 mapper 不直接被 controller 调用。
 */
@Mapper
public interface UniversityTagRelationMapper extends BaseMapper<UniversityTagRelation> {
}
