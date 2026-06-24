package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.entity.University;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UniversityMapper extends BaseMapper<University> {

    IPage<University> selectSearchPage(
            IPage<University> page,
            @Param("keyword") String keyword,
            @Param("continent") String continent,
            @Param("continentCountries") Set<String> continentCountries,
            @Param("country") String country,
            @Param("sortBy") String sortBy);

    List<String> selectCountries(
            @Param("continent") String continent,
            @Param("continentCountries") Set<String> continentCountries);
}
