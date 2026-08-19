package pvt.mktech.petcare.social.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JSON 字符串类型处理器。
 */
public class JsonStringTypeHandler extends BaseTypeHandler<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        validateJson(parameter);
        if (isPostgreSql(ps)) {
            ps.setObject(i, parameter, Types.OTHER);
        } else {
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }

    static boolean isPostgreSql(PreparedStatement statement) throws SQLException {
        return statement.getConnection().getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL");
    }

    private void validateJson(String value) throws SQLException {
        try {
            OBJECT_MAPPER.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Invalid JSON value", exception);
        }
    }
}
