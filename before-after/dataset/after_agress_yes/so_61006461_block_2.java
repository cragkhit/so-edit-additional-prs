 <dependency>
            <groupId>com.microsoft.azure</groupId>
            <artifactId>azure-storage</artifactId>
            <version>8.4.0</version>
        </dependency>
2. Update RSSFeedWriter class
public class RSSFeedWriter {
    private String outputFile; // the file name
    private Feed rssfeed;
    private String connectionString; // the storage account connection string
    public RSSFeedWriter(Feed rssfeed, String outputFile) {
        this.rssfeed = rssfeed;
        this.outputFile = outputFile;
    }
    public RSSFeedWriter(Feed rssfeed, String outputFile,String connectionString) {
        this.rssfeed = rssfeed;
        this.outputFile = outputFile;
        this.connectionString=connectionString;
    }
    public void write() throws Exception {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("test");
        CloudBlockBlob blob =container.getBlockBlobReference("test.rss");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         // create a XMLOutputFactory
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        // create XMLEventWriter
        XMLEventWriter eventWriter = outputFactory
                .createXMLEventWriter(outputStream);
        // create rss 
        ...
        // upload rss to Azure blob
        blob.upload(new ByteArrayInputStream(outputStream.toByteArray()),outputStream.toByteArray().length);
        outputStream.close();
    }
    private void createNode(XMLEventWriter eventWriter, String name,
                            String value) throws XMLStreamException {
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        XMLEvent tab = eventFactory.createDTD("\t");
        // create Start node
        StartElement sElement = eventFactory.createStartElement("", "", name);
        eventWriter.add(tab);
        eventWriter.add(sElement);
        // create Content
        Characters characters = eventFactory.createCharacters(value);
        eventWriter.add(characters);
        // create End node
        EndElement eElement = eventFactory.createEndElement("", "", name);
        eventWriter.add(eElement);
        eventWriter.add(end);
    }
}
3. update Function code
@FunctionName("HttpAddFeedItem")
public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {
    context.getLogger().info("Java HTTP trigger processed a request.");
 ...
// get the storage account connection string you store in app settings or local.settings.json with the key name
String connectionString = System.getenv("AzureWebJobsStorage"); 
        String outputFile=""; // the file name
        RSSFeedWriter writer = new RSSFeedWriter(rssFeeder, outputFile,connectionString);
        try {
            writer.write();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request.createResponseBuilder(HttpStatus.OK).body("success").build();
    }
}
----------
# update
If you want to directly access the rss file via the blob URL in the browser, please refer to the following steps
1. Please set the container public access level as `Public read access for blobs only`. For more details, please refer to the [document][2] 
2. Please ensure the blob's content type is right. The rss file's content type can be [`application/rss+xml`][3]. Regarding how to change the content type, please refer to the following code
 CloudStorageAccount storageAccount = CloudStorageAccount.parse(Constr)
CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("test");
        if(!container.exists()){
            container.create();
        }
 CloudBlockBlob blob =container.getBlockBlobReference("test.rss");
 blob.getProperties().setContentType("application/rss+xml");
        blob.uploadProperties();
After that, we can access the [file][4] in the browser 
[![enter image description here][5]][5]
[![enter image description here][6]][6]
  [1]: https://docs.microsoft.com/en-us/connectors/azureblob/
  [2]: https://docs.microsoft.com/en-us/azure/storage/blobs/storage-manage-access-to-resources
  [3]: https://stackoverflow.com/questions/595616/what-is-the-correct-mime-type-to-use-for-an-rss-feed
  [4]: https://andyprivate.blob.core.windows.net/test/test.rss
  [5]: https://i.stack.imgur.com/5FMxc.png
  [6]: https://i.stack.imgur.com/xveNe.png