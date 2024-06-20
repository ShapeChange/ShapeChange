/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 * 
 */
public class EAAccessConnection implements Closeable {

	private String dbJdbcDriver;
	private String dbDriver;
	private String dbURL;
	private String dbServiceName;
	private String dbuser;
	private String dbpassw;
	public Boolean isValid;

	protected Connection conn;

	public EAAccessConnection(String dbJdbcDriver, String dbDriver,
			String dbServiceName, String dbuser, String dbpassw)
			throws DatabaseException {

		this.dbJdbcDriver = dbJdbcDriver;
		this.dbDriver = dbDriver;
		this.dbServiceName = dbServiceName;
		this.dbuser = dbuser;
		this.dbpassw = dbpassw;

		isValid = true;
		initialise();
	}

	public void initialise() throws DatabaseException {

		conn = null;

		connect();

		close();
	}

	protected void connect() throws DatabaseException {

		if (conn == null) {

			try {
				File f = new File(dbServiceName);
				if (!f.exists())
					throw new IOException("Access-File " + dbServiceName
							+ " is not available.");
				dbURL = dbDriver + ";DBQ=" + dbServiceName + ";";

				// DB
				Class.forName(dbJdbcDriver);

				// open connection
				conn = DriverManager.getConnection(dbURL, dbuser, dbpassw);

			} catch (SQLException e) {
				throw new DatabaseException(e);
			} catch (IOException e) {
				throw new DatabaseException(e);
			} catch (ClassNotFoundException e) {
				throw new DatabaseException(e);
			}
		}
	}

	public void close() {
		// DB
		try {

			// close connection
			if (conn != null) {
				conn.close();
			}

		} catch (SQLException e) {
			e.printStackTrace(System.err);
		} finally {
			conn = null;
		}
	}

	public void establishAssociationClass(int connectorID, int elementID)
			throws DatabaseException {

		connect();

		try {

			String sql1 = "UPDATE t_connector SET SubType='Class' WHERE Connector_ID="
					+ connectorID;

			Statement s1 = conn.createStatement();
			s1.executeUpdate(sql1);

			String sql2 = "UPDATE t_connector SET PDATA1=" + elementID
					+ " WHERE Connector_ID=" + connectorID;

			Statement s2 = conn.createStatement();
			s2.executeUpdate(sql2);

			String sql3 = "UPDATE t_object SET PDATA4=" + connectorID
					+ " WHERE Object_ID=" + elementID;

			Statement s3 = conn.createStatement();
			s3.executeUpdate(sql3);

		} catch (SQLException e) {
			throw new DatabaseException(e.getMessage(), e);
		}
	}
}
