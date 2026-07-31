    @Override
    public void startEdit()
    {
        if ( !isEmpty() )
        {
            super.startEdit();
            createTextField();
            setText( null );
            setGraphic( textField );
            textField.requestFocus();
            textField.selectAll();
        }
    }