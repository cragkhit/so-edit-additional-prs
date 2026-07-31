<p>Edit: here is a piece of code that I use. I add that into a permission list in a RecyclerView.</p>
    if (!SystemProperties.get("ro.miui.ui.version.name").isEmpty() && Build.VERSION.SDK_INT >= 29) {
                // Android 10 MIUI 11
                PermissionData mPopup = new PermissionData();
                mPopup.text = "Other permissions > Display pop-up while in background";
                mPopup.onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                };
                mPermissionData.add(mPopup);
    }