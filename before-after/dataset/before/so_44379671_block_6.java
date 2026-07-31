    public class AddUserServlet extends HttpServlet {
    private DBJoint db;
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        db = (DBJoint) getServletContext().getAttribute("db");
		String Name = request.getParameter("name");
		String Login= request.getParameter("login");
		String Email= request.getParameter("email");
        db.getDBExecutor().addUser(
            new User(Name, Login, Email);
        //And I don't know what was that, it's 'success message' ok, but in your .jsp where is the 'serverAnswer' item?
        //req.setAttribute("serverAnswer", "Add ok!");
        req.getRequestDispatcher("answer.jsp").forward(req, resp);
        }    
    }