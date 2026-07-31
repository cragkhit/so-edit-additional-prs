    class ModeslSerializer extends JsonSerializer<List<Model>> {
    
    	@Override
    	public void serialize(List<Model> value, JsonGenerator jgen,
    			SerializerProvider provider) throws IOException,
    			JsonProcessingException {
    		jgen.writeStartArray();
    		for (Model model : value) {
    			jgen.writeStartObject();
    			jgen.writeObjectField("model", model);
    			jgen.writeEndObject();    
    		}
    		jgen.writeEndArray();
    	}
    
    }