package edu.isi.wings.execution.logger.api.impl.derby;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.Statement;
//import java.sql.ResultSet;
import java.util.Properties;

//import org.apache.derby.jdbc.EmbeddedDataSource;

public class DerbyDatabase {
	Properties props;
	private static Connection connect = null;
//	private EmbeddedDataSource ds;

	public DerbyDatabase(Properties props) {
		this.props = props;
		this.initializeDatabase();
		this.exQuery("INSERT INTO runs (x) values (" + Math.random() + ")");
		this.query("SELECT * from runs");
		//this.query("select * from sys.systables");
	}

	public void initializeDatabase() {
//		ds = new EmbeddedDataSource();
//		try {
//			if (connect == null) {
//				ds.setDatabaseName("rundb");
//				ds.setCreateDatabase("create");
//				connect = ds.getConnection();
//			}
//			//this.exQuery("drop table runs");
//			try {
//				this.exQuery("create table runs (x double)");
//			}
//			catch (Exception e) {
//				// Silently ignore
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
	}

	private void exQuery(String sql) {
		try {
			PreparedStatement st = connect.prepareStatement(sql);
			st.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void query(String sql) {
		try {
			PreparedStatement st = connect.prepareStatement(sql);
			st.executeQuery();
			ResultSet resultSet = st.executeQuery();
			while (resultSet.next()) {
				String x = resultSet.getString("x");
				System.out.println("x: " + x);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			connect.close();
//			ds.setShutdownDatabase("shutdown");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
