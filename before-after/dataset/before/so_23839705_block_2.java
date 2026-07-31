     public class UpdateFloorEventServlet extends HttpServlet {
    
    @Autowired
    private FloorService floorService;
    private WebApplicationContext springContext;
    @Override
    public void init(final ServletConfig config) throws ServletException {
           super.init(config);
           springContext = WebApplicationContextUtils.getRequieredWebApplicationContext(config.getServletContext());
           final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
           beanFactory.autowireBean(this);
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      }