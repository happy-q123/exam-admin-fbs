package com.domain.handler; // 记得修改为你的包名

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject; // 【关键】必须显式导入这个类

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * PostgreSQL pgvector 专用 TypeHandler
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgreSQLVectorTypeHandler extends AbstractJsonTypeHandler<List<Double>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 1. 构造函数：解决无参构造报错
    public PostgreSQLVectorTypeHandler(Class<?> type) {
        super(type);
    }

    public PostgreSQLVectorTypeHandler() {
        super(List.class);
    }

    // 2. 修复访问权限：必须是 public，不能是 protected
    @Override
    @SneakyThrows
    public List<Double> parse(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
    }

    // 3. 修复访问权限：必须是 public
    @Override
    @SneakyThrows
    public String toJson(List<Double> obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        pgObject.setValue(toJson(parameter));
        ps.setObject(i, pgObject);
    }
}