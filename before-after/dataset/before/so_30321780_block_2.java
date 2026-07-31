    private Context mContext
    @Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		mContext = activity;
	} 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.menu1_layout, container, false);
        //--Snippet
        mGoogleApiClient = new GoogleApiClient
                .Builder(mContext )
                .enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        return v;
    }