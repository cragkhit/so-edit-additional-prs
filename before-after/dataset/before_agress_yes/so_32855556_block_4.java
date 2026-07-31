    private void closeResources(Connection con, PreparedStatement ps) {
    	try {
    		if (ps != null) {
   				ps.close();
   			}
   		} catch (SQLException e) {}
   		try {
   			if (con != null) {
   				con.close();
   			}
   		} catch (SQLException e) {}
    }