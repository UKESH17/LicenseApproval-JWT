package com.htc.licenseapproval.entity.idgenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class CustomHeaderGenerator implements IdentifierGenerator {

    private static final long serialVersionUID = 1L;
    private static final String PREFIX = "req-head-";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        String updateQuery = "UPDATE header_id_counter SET current_value = current_value + 1";
        String selectQuery = "SELECT current_value FROM header_id_counter";

        try (Connection connection = session.getJdbcConnectionAccess().obtainConnection()) {
            connection.setAutoCommit(false);

            try (
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                PreparedStatement selectStmt = connection.prepareStatement(selectQuery)
            ) {
                updateStmt.executeUpdate();

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("current_value");
                        connection.commit();
                        return PREFIX + id;
                    } else {
                        connection.rollback();
                        throw new RuntimeException("Failed to fetch updated ID.");
                    }
                }

            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException("Exception during ID generation", ex);
            } finally {
                connection.setAutoCommit(true);
                
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error while generating ID", e);
        }
    }
}
