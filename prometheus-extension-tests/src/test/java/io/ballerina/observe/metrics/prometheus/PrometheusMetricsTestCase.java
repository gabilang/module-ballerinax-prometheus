/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.metrics.prometheus;

import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.context.LogLeecher;
import org.ballerinalang.test.context.Utils;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Integration test for Prometheus extension.
 */
public class PrometheusMetricsTestCase extends BaseTestCase {
    private BServerInstance serverInstance;

    private static final File RESOURCES_DIR = Paths.get("src", "test", "resources", "bal").toFile();
    private static final Pattern PROMETHEUS_METRIC_VALUE_REGEX = Pattern.compile("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?");
    private static final String TEST_RESOURCE_URL = "http://localhost:9091/test/sum";

    private static final String PROMETHEUS_EXTENSION_LOG_PREFIX = "ballerina: started Prometheus HTTP listener ";

    @BeforeMethod
    public void setup() throws Exception {
        serverInstance = new BServerInstance(balServer);
    }

    @AfterMethod
    public void cleanUpServer() throws Exception {
        serverInstance.shutdownServer();
    }

    @DataProvider(name = "test-prometheus-metrics-data")
    public Object[][] getTestPrometheusMetricsData() {
        return new Object[][]{
                {"0.0.0.0:9797", "http://localhost:9797/metrics", 9797, "ConfigDefault.toml"},
                {"127.0.0.1:10097", "http://127.0.0.1:10097/metrics", 10097, "ConfigOverridden.toml"}
        };
    }

    @Test(dataProvider = "test-prometheus-metrics-data")
    public void testPrometheusMetrics(String prometheusServiceBindAddress, String prometheusScrapeURL,
                                      int prometheusPort, String configFilename) throws Exception {
        final Map<String, Pattern> expectedMetrics = new HashMap<>();
        expectedMetrics.put("requests_total_value{listener_name=\"http\",src_module=\"_anon/.:0.0.0\"," +
                "src_object_name=\"/test\",http_status_code_group=\"2xx\",src_position=\"01_http_svc_test.bal:21:5\"," +
                "protocol=\"http\",entrypoint_resource_accessor=\"get\",src_service_resource=\"true\"," +
                "src_resource_path=\"/sum\",src_resource_accessor=\"get\",entrypoint_function_name=\"/sum\"," +
                "entrypoint_function_module=\"_anon/.:0.0.0\",entrypoint_service_name=\"/test\"," +
                "http_url=\"/test/sum\",http_method=\"GET\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("requests_total_value{src_object_name=\"ballerina/http/Caller\"," +
                "src_module=\"_anon/.:0.0.0\",http_status_code_group=\"2xx\",src_client_remote=\"true\"," +
                "src_position=\"01_http_svc_test.bal:27:20\",entrypoint_function_name=\"/sum\"," +
                "src_function_name=\"respond\",entrypoint_function_module=\"_anon/.:0.0.0\"," +
                "entrypoint_service_name=\"/test\",entrypoint_resource_accessor=\"get\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("inprogress_requests_value{listener_name=\"http\",src_module=\"_anon/.:0.0.0\"," +
                "src_object_name=\"/test\",src_position=\"01_http_svc_test.bal:21:5\",protocol=\"http\"," +
                "entrypoint_resource_accessor=\"get\",src_service_resource=\"true\",src_resource_path=\"/sum\"," +
                "src_resource_accessor=\"get\",entrypoint_function_name=\"/sum\"," +
                "entrypoint_function_module=\"_anon/.:0.0.0\",entrypoint_service_name=\"/test\"," +
                "http_url=\"/test/sum\",http_method=\"GET\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("inprogress_requests_value{src_object_name=\"ballerina/http/Caller\"," +
                "src_module=\"_anon/.:0.0.0\",src_client_remote=\"true\"," +
                "src_position=\"01_http_svc_test.bal:27:20\",entrypoint_function_name=\"/sum\"," +
                "src_function_name=\"respond\",entrypoint_function_module=\"_anon/.:0.0.0\"," +
                "entrypoint_service_name=\"/test\",entrypoint_resource_accessor=\"get\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("response_time_nanoseconds_total_value{listener_name=\"http\"," +
                "src_module=\"_anon/.:0.0.0\",src_object_name=\"/test\",http_status_code_group=\"2xx\"," +
                "src_position=\"01_http_svc_test.bal:21:5\",protocol=\"http\",entrypoint_resource_accessor=\"get\"," +
                "src_service_resource=\"true\",src_resource_path=\"/sum\",src_resource_accessor=\"get\"," +
                "entrypoint_function_name=\"/sum\",entrypoint_function_module=\"_anon/.:0.0.0\"," +
                "entrypoint_service_name=\"/test\",http_url=\"/test/sum\",http_method=\"GET\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("response_time_nanoseconds_total_value{src_object_name=\"ballerina/http/Caller\"," +
                "src_module=\"_anon/.:0.0.0\",http_status_code_group=\"2xx\",src_client_remote=\"true\"," +
                "src_position=\"01_http_svc_test.bal:27:20\",entrypoint_function_name=\"/sum\"," +
                "src_function_name=\"respond\",entrypoint_function_module=\"_anon/.:0.0.0\"," +
                "entrypoint_service_name=\"/test\",entrypoint_resource_accessor=\"get\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("response_time_seconds_value{listener_name=\"http\",src_module=\"_anon/.:0.0.0\"," +
                "src_object_name=\"/test\",http_status_code_group=\"2xx\",src_position=\"01_http_svc_test.bal:21:5\"," +
                "protocol=\"http\",entrypoint_resource_accessor=\"get\",src_service_resource=\"true\"," +
                "src_resource_path=\"/sum\",src_resource_accessor=\"get\",entrypoint_function_name=\"/sum\"," +
                "entrypoint_function_module=\"_anon/.:0.0.0\",entrypoint_service_name=\"/test\"," +
                "http_url=\"/test/sum\",http_method=\"GET\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);
        expectedMetrics.put("response_time_seconds_value{src_object_name=\"ballerina/http/Caller\"," +
                "src_module=\"_anon/.:0.0.0\",http_status_code_group=\"2xx\",src_client_remote=\"true\"," +
                "src_position=\"01_http_svc_test.bal:27:20\",entrypoint_function_name=\"/sum\"," +
                "src_function_name=\"respond\",entrypoint_function_module=\"_anon/.:0.0.0\"," +
                "entrypoint_service_name=\"/test\",entrypoint_resource_accessor=\"get\",}",
                PROMETHEUS_METRIC_VALUE_REGEX);

        LogLeecher prometheusExtLogLeecher = new LogLeecher(PROMETHEUS_EXTENSION_LOG_PREFIX
                + prometheusServiceBindAddress);
        serverInstance.addLogLeecher(prometheusExtLogLeecher);
        LogLeecher errorLogLeecher = new LogLeecher("error");
        serverInstance.addErrorLogLeecher(errorLogLeecher);
        LogLeecher exceptionLogLeecher = new LogLeecher("Exception");
        serverInstance.addErrorLogLeecher(exceptionLogLeecher);

        String configFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), configFilename).toFile().getAbsolutePath();
        Map<String, String> env = new HashMap<>();
        env.put("BAL_CONFIG_FILES", configFile);

        final String balFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "01_http_svc_test.bal").toFile()
                .getAbsolutePath();
        int[] requiredPorts = {9091, prometheusPort};
        serverInstance.startServer(balFile, new String[]{"--observability-included"},
                null, env, requiredPorts);
        Utils.waitForPortsToOpen(requiredPorts, 1000 * 60, false, "localhost");
        prometheusExtLogLeecher.waitForText(10000);

        // Send requests to generate metrics
        int i = 0;
        while (i < 5) {
            HttpResponse response = HttpClientRequest.doGet(TEST_RESOURCE_URL);
            Assert.assertEquals(response.getResponseCode(), 200);
            String responseData = response.getData();
            Assert.assertEquals(responseData, "Sum: 53");
            i++;
        }
        Thread.sleep(1000);

        // Read metrics from Prometheus endpoint
        HttpResponse response = HttpClientRequest.doGetAndPreserveNewlineInResponseData(prometheusScrapeURL);
        Assert.assertEquals(response.getResponseCode(), 200);
        List<String> metricsList = response.getData().lines()
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        Assert.assertTrue(metricsList.size() != 0);
        int count = 0;
        for (String line : metricsList) {
            int index = line.lastIndexOf(" ");
            String key = line.substring(0, index);
            String value = line.substring(index + 1);
            Pattern pattern = expectedMetrics.get(key);
            if (pattern != null) {
                count++;
                Assert.assertTrue(pattern.matcher(value).find(),
                        "Unexpected value found for metric " + key + ". Value: " + value + ", Pattern: "
                                + pattern.pattern() + " Complete line: " + line);
            }
        }
        Assert.assertEquals(count, expectedMetrics.size(), "Metrics count is not equal to the expected metrics count.");
        Assert.assertFalse(errorLogLeecher.isTextFound(), "Unexpected error log found");
        Assert.assertFalse(exceptionLogLeecher.isTextFound(), "Unexpected exception log found");
    }

    @Test
    public void testPrometheusDisabled() throws Exception {
        LogLeecher prometheusExtLogLeecher = new LogLeecher(PROMETHEUS_EXTENSION_LOG_PREFIX);
        serverInstance.addLogLeecher(prometheusExtLogLeecher);
        LogLeecher errorLogLeecher = new LogLeecher("error");
        serverInstance.addErrorLogLeecher(errorLogLeecher);
        LogLeecher exceptionLogLeecher = new LogLeecher("Exception");
        serverInstance.addErrorLogLeecher(exceptionLogLeecher);

        final String balFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "01_http_svc_test.bal").toFile()
                .getAbsolutePath();
        int[] requiredPorts = {9091};
        serverInstance.startServer(balFile, null, null, requiredPorts);
        Utils.waitForPortsToOpen(requiredPorts, 1000 * 60, false, "localhost");

        String responseData = HttpClientRequest.doGet(TEST_RESOURCE_URL).getData();
        Assert.assertEquals(responseData, "Sum: 53");

        Assert.assertFalse(prometheusExtLogLeecher.isTextFound(), "Prometheus extension not expected to enable");
        Assert.assertFalse(errorLogLeecher.isTextFound(), "Unexpected error log found");
        Assert.assertFalse(exceptionLogLeecher.isTextFound(), "Unexpected exception log found");
    }
}
