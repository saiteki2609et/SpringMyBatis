package com.example.scm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.scm.domain.Warehouse;

/**
 * 【MyBatis 学習ポイント: 一番シンプルな形】
 *
 * インタフェースに {@code @Mapper} を付けるだけで Spring の Bean になる。
 * 実装クラスは MyBatis が動的プロキシで生成するので自分で書かない。
 */
@Mapper
public interface WarehouseMapper {

    @Select("SELECT id, code, name, address FROM warehouse ORDER BY code")
    List<Warehouse> findAll();

    @Select("SELECT id, code, name, address FROM warehouse WHERE id = #{id}")
    Warehouse findById(Integer id);
}
