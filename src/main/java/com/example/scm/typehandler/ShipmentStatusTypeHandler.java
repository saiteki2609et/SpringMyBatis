package com.example.scm.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.example.scm.domain.ShipmentStatus;

/**
 * 【MyBatis 学習ポイント: TypeHandler】
 *
 * Java の enum と DB のコード値(VARCHAR "10"/"20"...)を相互変換する。
 * application.yml の {@code mybatis.type-handlers-package} で
 * このパッケージを指定しているため、ShipmentStatus 型のカラムには
 * 自動的にこのハンドラが使われる(XML 側に typeHandler= を書かなくてよい)。
 *
 * レガシーな業務DBは「区分値をコードで持つ」ことが多いので、
 * 実案件でも一番出番の多いカスタム TypeHandler パターン。
 */
@MappedTypes(ShipmentStatus.class)
public class ShipmentStatusTypeHandler extends BaseTypeHandler<ShipmentStatus> {

    /** Java → DB (INSERT/UPDATE のパラメータ、WHERE 句の値) */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ShipmentStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    /** DB → Java (カラム名で取得) */
    @Override
    public ShipmentStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toStatus(rs.getString(columnName));
    }

    /** DB → Java (カラム位置で取得) */
    @Override
    public ShipmentStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toStatus(rs.getString(columnIndex));
    }

    /** DB → Java (ストアドプロシージャの OUT パラメータ) */
    @Override
    public ShipmentStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toStatus(cs.getString(columnIndex));
    }

    private ShipmentStatus toStatus(String code) {
        return code == null ? null : ShipmentStatus.fromCode(code);
    }
}
