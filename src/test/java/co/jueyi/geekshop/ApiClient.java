package co.jueyi.geekshop;

import co.jueyi.geekshop.common.Constant;
import co.jueyi.geekshop.common.RoleCode;
import co.jueyi.geekshop.options.ConfigOptions;
import co.jueyi.geekshop.options.SuperadminCredentials;
import co.jueyi.geekshop.service.ConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  A minimalistic GraphQL client for populating and querying test data.
 *
 *  adapted from {@link com.graphql.spring.boot.test.GraphQLTestTemplate}
 *
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public class ApiClient {
    public static final String AUTH_GRAPHQL_RESOURCE_TEMPLATE = "graphql/%s.graphqls";

    public static final String ADMIN_LOGIN =
            String.format(AUTH_GRAPHQL_RESOURCE_TEMPLATE, "admin_login");
    public static final String ADMIN_LOGOUT =
            String.format(AUTH_GRAPHQL_RESOURCE_TEMPLATE, "admin_logout");
    public static final String LOGIN =
            String.format(AUTH_GRAPHQL_RESOURCE_TEMPLATE, "login");
    public static final String LOGOUT =
            String.format(AUTH_GRAPHQL_RESOURCE_TEMPLATE, "logout");

    @Autowired
    private ConfigService configService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired(required = false)
    private TestRestTemplate restTemplate;
    @Value("${graphql.servlet.mapping:/graphql}")
    private String graphqlMapping;
    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    private HttpHeaders headers = new HttpHeaders();

    private String createJsonQuery(String graphql, ObjectNode variables)
            throws JsonProcessingException {

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("query", graphql);
        wrapper.set("variables", variables);
        return objectMapper.writeValueAsString(wrapper);
    }

    private String loadQuery(String location) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + location);
        return loadResource(resource);
    }

    private String loadResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Add an HTTP header that will be sent with each request this sends.
     *
     * @param name Name (key) of HTTP header to add.
     * @param value Value of HTTP header to add.
     */
    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    /**
     * Replace any associated HTTP headers with the provided headers.
     *
     * @param newHeaders Headers to use.
     */
    public void setHeaders(HttpHeaders newHeaders) {
        headers = newHeaders;
    }

    /**
     * Clear all associated HTTP headers.
     */
    public void clearHeaders() {
        setHeaders(new HttpHeaders());
    }

    /**
     * @deprecated Use {@link #postForResource(String)} instead
     *
     * @param graphqlResource path to the classpath resource containing the GraphQL query
     * @return GraphQLResponse containing the result of query execution
     * @throws IOException if the resource cannot be loaded from the classpath
     */
    public GraphQLResponse perform(String graphqlResource) throws IOException {
        return postForResource(graphqlResource);
    }


    /**
     * Loads a GraphQL query from the given classpath resource and sends it to the GraphQL server.
     *
     * @param graphqlResource path to the classpath resource containing the GraphQL query
     * @return GraphQLResponse containing the result of query execution
     * @throws IOException if the resource cannot be loaded from the classpath
     */
    public GraphQLResponse postForResource(String graphqlResource) throws IOException {
        return perform(graphqlResource, null);
    }

    /**
     * Loads a GraphQL query from the given classpath resource, appending any graphql fragment
     * resources provided  and sends it to the GraphQL server.
     *
     * @param graphqlResource path to the classpath resource containing the GraphQL query
     * @param fragmentResources an ordered list of classpaths containing GraphQL fragment definitions.
     * @return GraphQLResponse containing the result of query execution
     * @throws IOException if the resource cannot be loaded from the classpath
     */
    public GraphQLResponse postForResource(String graphqlResource, List<String> fragmentResources) throws IOException {
        return perform(graphqlResource, null, fragmentResources);
    }

    static HttpEntity<Object> forJson(String json, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    static HttpEntity<Object> forMultipart(String query, String variables, HttpHeaders headers) {
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
        values.add("query", forJson(query, new HttpHeaders()));
        values.add("variables", forJson(variables, new HttpHeaders()));
        return new HttpEntity<>(values, headers);
    }

    public GraphQLResponse postMultipart(String query, String variables) {
        return postRequest(forMultipart(query, variables, headers));
    }

    private GraphQLResponse post(String payload) {
        return postRequest(forJson(payload, headers));
    }

    private GraphQLResponse postRequest(HttpEntity<Object> request) {
        ResponseEntity<String> response = restTemplate.exchange(graphqlMapping, HttpMethod.POST, request, String.class);
        return new GraphQLResponse(response, objectMapper);
    }


    public void setAuthToken(String token) {
        this.authToken = token;
        if (!StringUtils.isEmpty(this.authToken)) {
            headers.remove(Constant.HTTP_HEADER_AUTHORIZATION);
            headers.add(Constant.HTTP_HEADER_AUTHORIZATION, "Bearer " + this.authToken);
        } else {
            headers.remove(Constant.HTTP_HEADER_AUTHORIZATION);
        }
    }

    public String getAuthToken() {
        return this.authToken;
    }

    public void asCustomerUserWithCredentials(String username, String password) throws IOException {
        asUserWithCredentials(username, password, false);
    }

    public void asAdminUserWithCredentials(String username, String password) throws IOException {
        asUserWithCredentials(username, password, true);
    }

    /**
     * Attempts to log in with the specified credentials.
     */
    private void asUserWithCredentials(String username, String password, boolean admin) throws IOException {
        // first log out as the current user
        if (!StringUtils.isEmpty(this.authToken)) {
            perform(admin? ADMIN_LOGOUT : LOGOUT, null);
        }
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("username", username);
        variables.put("password", password);
        GraphQLResponse response = perform(admin? ADMIN_LOGIN : LOGIN, variables);
        assertThat(response.isOk());
    }

    /**
     * Logs out so that the client is then treated as an anonymous user.
     */
    public void asAnonymousUser() throws IOException {
        perform(ADMIN_LOGOUT, null);
        perform(LOGOUT, null);
    }

    /**
     * Logs in as the SuperAdmin user.
     */
    public void asSuperAdmin() throws IOException {
        SuperadminCredentials superadminCredentials = this.configService.getAuthOptions().getSuperadminCredentials();
        this.asUserWithCredentials(
                superadminCredentials.getIdentifier(),
                superadminCredentials.getPassword(),
                true);
    }

    /**
     * 支持抛出异常的perform
     *
     * @param graphqlResource
     * @param variables
     * @return
     * @throws IOException
     */
    public GraphQLResponse perform(String graphqlResource, ObjectNode variables) throws IOException {
        String graphql = loadQuery(graphqlResource);
        String payload = createJsonQuery(graphql, variables);
        GraphQLResponse response = post(payload);
        return handleResponse(response);
    }

    /**
     * 支持抛出异常和GraphQL Fragment的perform
     *
     * @param graphqlResource
     * @param variables
     * @param fragmentResources
     * @return
     * @throws IOException
     */
    public GraphQLResponse perform(String graphqlResource, ObjectNode variables, List<String> fragmentResources)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String fragmentResource : fragmentResources) {
            sb.append(loadQuery(fragmentResource));
        }
        String graphql = sb.append(loadQuery(graphqlResource)).toString();
        String payload = createJsonQuery(graphql, variables);
        GraphQLResponse response = post(payload);
        return handleResponse(response);
    }

//    /**
//     * 参考：
//     * https://medium.com/red6-es/uploading-a-file-with-a-filename-with-spring-resttemplate-8ec5e7dc52ca
//     * https://github.com/jaydenseric/graphql-multipart-request-spec
//     *
//     * @param path
//     * @param mediaType
//     * @return
//     * @throws IOException
//     */
//    public GraphQLResponse uploadMultipleFiles(String path, String mediaType) throws IOException {
//        File dir = new File(path);
//        if (!dir.exists()) throw new IOException("Path [" + path + "] does not exist");
//        List<File> fileList = Arrays.asList(dir.listFiles());
//        int size = fileList.size();
//        if (size == 0) throw new IOException("No file exists in path [" + path + "]");
//
//        String graphql = loadQuery(CREATE_ASSETS);
//        ObjectNode variables = objectMapper.createObjectNode();
//        ArrayNode fileArray = objectMapper.createArrayNode();
//        for (int i = 0; i < size; i++) {
//            fileArray.addNull();
//        }
//        variables.set("files", fileArray);
//        String jsonQuery = createJsonQuery(graphql, variables);
//
//        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add("operations", forJson(jsonQuery, new HttpHeaders()));
//
//        ObjectNode map = objectMapper.createObjectNode();
//        for (int i = 0; i < size; i++) {
//            map.set("" + i, objectMapper.createArrayNode().add("variables.files." + i));
//        }
//        String jsonMap = objectMapper.writeValueAsString(map);
//        body.add("map", forJson(jsonMap, new HttpHeaders()));
//
//        for(int i = 0; i < size; i++) {
//            File file = fileList.get(i);
//            Resource resource = new FileSystemResource(file);
//            String fileName = file.getName();
//
//            MultiValueMap<String, String> fileMeta = new LinkedMultiValueMap<>();
//            ContentDisposition contentDisposition = ContentDisposition
//                    .builder("form-data").name("" + i).filename(fileName).build();
//            fileMeta.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//            fileMeta.add(HttpHeaders.CONTENT_TYPE, mediaType); // MediaType.IMAGE_JPEG_VALUE
//            body.add("" + i, new HttpEntity<>(resource, fileMeta));
//        }
//
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        HttpEntity httpEntity = new HttpEntity<>(body, headers);
//
//        return postRequest(httpEntity);
//    }

//    /**
//     * 参考：
//     * https://medium.com/red6-es/uploading-a-file-with-a-filename-with-spring-resttemplate-8ec5e7dc52ca
//     * https://github.com/jaydenseric/graphql-multipart-request-spec
//     *
//     *
//     * @param fileName
//     * @param mediaType
//     * @return
//     * @throws IOException
//     */
//    public GraphQLResponse uploadSingleFile(String fileName, String mediaType)
//            throws IOException {
//        String graphql = loadQuery(CREATE_ASSET);
//        ObjectNode variables = objectMapper.createObjectNode();
//        variables.set("file", null);
//        String jsonQuery = createJsonQuery(graphql, variables);
//
//        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add("operations", forJson(jsonQuery, new HttpHeaders()));
//
//        ObjectNode map = objectMapper.createObjectNode();
//        map.set("0", objectMapper.createArrayNode().add("variables.file"));
//        String jsonMap = objectMapper.writeValueAsString(map);
//        body.add("map", forJson(jsonMap, new HttpHeaders()));
//
//        File imageFile = new File(MockDataService.TEST_ASSET_PATH + fileName);
//        Resource resource = new FileSystemResource(imageFile);
//
//        MultiValueMap<String, String> fileMeta = new LinkedMultiValueMap<>();
//        ContentDisposition contentDisposition = ContentDisposition
//                .builder("form-data").name("0").filename(fileName).build();
//        fileMeta.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//        fileMeta.add(HttpHeaders.CONTENT_TYPE, mediaType); // MediaType.IMAGE_JPEG_VALUE
//        body.add("0", new HttpEntity<>(resource, fileMeta));
//
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        HttpEntity httpEntity = new HttpEntity<>(body, headers);
//
//        return postRequest(httpEntity);
//    }

    private GraphQLResponse handleResponse(GraphQLResponse response) throws IOException {
        boolean hasErrors = response.readTree().has("errors");
        if (hasErrors) {
            String errorMessage = response.get("$.errors[0].message");
            String errorCode = "None";
            try {
                errorCode = response.get("$.errors[0].extensions.errorCode");
            } catch (PathNotFoundException ex) {
                errorCode = response.get("$.errors[0].extensions.classification");
            }
            throw new ApiException(errorCode, errorMessage);
        }
        String authTokenHeaderKey = this.configService.getAuthOptions().getAuthTokenHeaderKey();
        String authToken = response.getRawResponse().getHeaders().getFirst(authTokenHeaderKey);
        if (authToken != null) {
            this.setAuthToken(authToken);
        }
        return response;
    }
}
