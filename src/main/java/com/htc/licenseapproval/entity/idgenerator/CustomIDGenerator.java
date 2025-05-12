package com.htc.licenseapproval.entity.idgenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;

import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.enums.LicenseType;

public class CustomIDGenerator implements Generator, IdentifierGenerator {

	private static final long serialVersionUID = 1L;

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) {
		RequestDetails requestDetails = (RequestDetails) object;
		LicenseType licenceType = requestDetails.getLicenseDetails().getLicenseType();
		String PREFIX = licenceType.equals(LicenseType.PLURALS) ? "OEM-PLU-" : "OEM-LIN-";
		String updateQuery = "UPDATE license_id_counter SET current_value = current_value + 1 WHERE license_type = ?";
		String selectQuery = "SELECT current_value FROM license_id_counter WHERE license_type = ?";
		try (Connection connection = session.getJdbcConnectionAccess().obtainConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
					PreparedStatement selectStmt = connection.prepareStatement(selectQuery);) {

				updateStmt.setString(1, licenceType.toString());
				updateStmt.executeUpdate();

				selectStmt.setString(1, licenceType.toString());
				ResultSet rs = selectStmt.executeQuery();

				if (rs.next()) {
					int id = rs.getInt("current_value");
					connection.commit();
					return PREFIX + id;
				} else {
					connection.rollback();
					throw new RuntimeException("License type not found in counter table.");
				}

			} catch (Exception ex) {
				connection.rollback();
				throw ex;
			} finally {
				connection.setAutoCommit(true);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Something went wrong !!");
	}

}
