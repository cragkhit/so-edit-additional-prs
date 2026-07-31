        import java.io.IOException;
        import java.util.Map;
        
        import cafe.image.meta.Metadata;
        import cafe.image.meta.MetadataType;
        import cafe.image.meta.icc.ICCProfile;
        
        public class ExtractICCProfile {
        
        	public static void main(String[] args) throws IOException {
        		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
        		ICCProfile icc_profile = (ICCProfile)metadataMap.get(MetadataType.ICC_PROFILE);
        		
        		if(icc_profile != null) {
        			icc_profile.showMetadata();
        		}	
        	}	
        }