// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.dicomwebfuse.fuse;

import static com.google.dicomwebfuse.EntityType.DICOM_STORE;
import static com.google.dicomwebfuse.EntityType.INSTANCE;
import static com.google.dicomwebfuse.EntityType.SERIES;
import static com.google.dicomwebfuse.EntityType.STUDY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import com.google.dicomwebfuse.TestUtils;
import com.google.dicomwebfuse.auth.AuthAdc;
import com.google.dicomwebfuse.dao.FuseDao;
import com.google.dicomwebfuse.dao.FuseDaoImpl;
import com.google.dicomwebfuse.dao.http.HttpClientFactory;
import com.google.dicomwebfuse.entities.CloudConf;
import com.google.dicomwebfuse.entities.DicomPath;
import com.google.dicomwebfuse.entities.cache.Cache;
import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.fuse.cacher.DicomPathCacher;
import com.google.dicomwebfuse.parser.Arguments;
import java.io.IOException;
import jnr.ffi.Platform;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DicomFuseHelperTest {

  @Test
  void testShouldSuccessfullyOpenUnlistedStudy() throws IOException, DicomFuseException {
    CloseableHttpClient closeableHttpClient = Mockito.mock(CloseableHttpClient.class);

    HttpClientFactory httpClientFactory = TestUtils.prepareHttpClientFactory(closeableHttpClient);
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, DICOM_STORE,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/", null);
    String studiesPath =
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/";
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 0, STUDY,
        HttpStatusCodes.STATUS_CODE_OK, studiesPath, "limit=5000&offset=0");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 5000, STUDY,
        HttpStatusCodes.STATUS_CODE_OK, studiesPath, "limit=5000&offset=5000");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 10000, STUDY,
        HttpStatusCodes.STATUS_CODE_OK, studiesPath, "limit=5000&offset=10000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, STUDY,
        HttpStatusCodes.STATUS_CODE_OK, studiesPath, "limit=5000&offset=15000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, STUDY,
        HttpStatusCodes.STATUS_CODE_OK, studiesPath, "StudyInstanceUID=15001");

    Cache cache = new Cache();
    DicomFuseHelper dicomFuseHelper = prepareDicomFuseHelper(httpClientFactory, cache);

    DicomPathCacher dicomPathCacher = new DicomPathCacher();
    DicomPathParser dicomPathParser = new DicomPathParser(dicomPathCacher);

    // caching all DICOM Stores in the current Dataset
    DicomPath datasetPath = dicomPathParser.parsePath("/");
    dicomFuseHelper.updateDir(datasetPath);
    // caching all Studies in the current DICOM Store
    DicomPath dicomStorePath = dicomPathParser.parsePath("/test1");
    dicomFuseHelper.updateDir(dicomStorePath);
    // checking that unlisted Study is not in the cache
    DicomPath unlistedStudyPath = dicomPathParser.parsePath("/test1/15001");
    assertTrue(cache.isStudyNotExist(unlistedStudyPath));
    // caching unlisted Study
    dicomFuseHelper.checkExistingObject(unlistedStudyPath);
    // checking that Study is in the cache
    assertFalse(cache.isStudyNotExist(unlistedStudyPath));
  }

  @Test
  void testShouldSuccessfullyOpenUnlistedSeries() throws IOException, DicomFuseException {
    CloseableHttpClient closeableHttpClient = Mockito.mock(CloseableHttpClient.class);

    HttpClientFactory httpClientFactory = TestUtils.prepareHttpClientFactory(closeableHttpClient);
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, DICOM_STORE,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/", null);
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, STUDY,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/",
        "limit=5000&offset=0");
    String seriesPath =
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/1/series/";
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 0, SERIES,
        HttpStatusCodes.STATUS_CODE_OK, seriesPath,
        "includefield=0020000D&limit=5000&offset=0");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 5000, SERIES,
        HttpStatusCodes.STATUS_CODE_OK, seriesPath,
        "includefield=0020000D&limit=5000&offset=5000");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 10000, SERIES,
        HttpStatusCodes.STATUS_CODE_OK, seriesPath,
        "includefield=0020000D&limit=5000&offset=10000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, SERIES,
        HttpStatusCodes.STATUS_CODE_OK, seriesPath,
        "includefield=0020000D&limit=5000&offset=15000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, SERIES,
        HttpStatusCodes.STATUS_CODE_OK, seriesPath,
        "includefield=0020000D&SeriesInstanceUID=15001");

    Cache cache = new Cache();
    DicomFuseHelper dicomFuseHelper = prepareDicomFuseHelper(httpClientFactory, cache);

    DicomPathCacher dicomPathCacher = new DicomPathCacher();
    DicomPathParser dicomPathParser = new DicomPathParser(dicomPathCacher);

    // caching all DICOM Stores in the current Dataset
    DicomPath datasetPath = dicomPathParser.parsePath("/");
    dicomFuseHelper.updateDir(datasetPath);
    // caching all Studies in the current DICOM Store
    DicomPath dicomStorePath = dicomPathParser.parsePath("/test1");
    dicomFuseHelper.updateDir(dicomStorePath);
    // caching all Series in the current Study
    DicomPath studyPath = dicomPathParser.parsePath("/test1/1");
    dicomFuseHelper.updateDir(studyPath);
    // checking that unlisted Series is not in the cache
    DicomPath unlistedSeriesPath = dicomPathParser.parsePath("/test1/1/15001");
    assertTrue(cache.isSeriesNotExist(unlistedSeriesPath));
    // caching unlisted Series
    dicomFuseHelper.checkExistingObject(unlistedSeriesPath);
    // checking that Series is in the cache
    assertFalse(cache.isSeriesNotExist(unlistedSeriesPath));
  }

  @Test
  void testShouldSuccessfullyOpenUnlistedInstance() throws IOException, DicomFuseException {
    CloseableHttpClient closeableHttpClient = Mockito.mock(CloseableHttpClient.class);

    HttpClientFactory httpClientFactory = TestUtils.prepareHttpClientFactory(closeableHttpClient);
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, DICOM_STORE,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/", null);
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, STUDY,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/",
        "limit=5000&offset=0");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 0, SERIES,
        HttpStatusCodes.STATUS_CODE_OK,
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/1/series/",
        "includefield=0020000D&limit=5000&offset=0");
    String instancesPath =
        "/test/projects/test/locations/test/datasets/test/dicomStores/test1/dicomWeb/studies/1/series/1/instances/";
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 0, INSTANCE,
        HttpStatusCodes.STATUS_CODE_OK, instancesPath,
        "includefield=0020000D&includefield=0020000E&limit=5000&offset=0");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 5000, INSTANCE,
        HttpStatusCodes.STATUS_CODE_OK, instancesPath,
        "includefield=0020000D&includefield=0020000E&limit=5000&offset=5000");
    TestUtils.prepareHttpClient(closeableHttpClient, 5000, 10000, INSTANCE,
        HttpStatusCodes.STATUS_CODE_OK, instancesPath,
        "includefield=0020000D&includefield=0020000E&limit=5000&offset=10000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, INSTANCE,
        HttpStatusCodes.STATUS_CODE_OK, instancesPath,
        "includefield=0020000D&includefield=0020000E&limit=5000&offset=15000");
    TestUtils.prepareHttpClient(closeableHttpClient, 1, 15000, INSTANCE,
        HttpStatusCodes.STATUS_CODE_OK, instancesPath,
        "includefield=0020000D&includefield=0020000E&SOPInstanceUID=15001");

    Cache cache = new Cache();
    DicomFuseHelper dicomFuseHelper = prepareDicomFuseHelper(httpClientFactory, cache);

    DicomPathCacher dicomPathCacher = new DicomPathCacher();
    DicomPathParser dicomPathParser = new DicomPathParser(dicomPathCacher);

    // caching all DICOM Stores in the current Dataset
    DicomPath datasetPath = dicomPathParser.parsePath("/");
    dicomFuseHelper.updateDir(datasetPath);
    // caching all Studies in the current DICOM Store
    DicomPath dicomStorePath = dicomPathParser.parsePath("/test1");
    dicomFuseHelper.updateDir(dicomStorePath);
    // caching all Series in the current Study
    DicomPath studyPath = dicomPathParser.parsePath("/test1/1");
    dicomFuseHelper.updateDir(studyPath);
    // caching all Instances in the current Series
    DicomPath seriesPath = dicomPathParser.parsePath("/test1/1/1");
    dicomFuseHelper.updateDir(seriesPath);
    // checking that unlisted Instance is not in the cache
    DicomPath unlistedInstancePath = dicomPathParser.parsePath("/test1/1/1/15001");
    assertTrue(cache.isInstanceNotExist(unlistedInstancePath));
    // caching unlisted Instance
    dicomFuseHelper.checkExistingObject(unlistedInstancePath);
    // checking that Instance is in the cache
    assertFalse(cache.isSeriesNotExist(unlistedInstancePath));
  }

  private DicomFuseHelper prepareDicomFuseHelper(HttpClientFactory httpClientFactory, Cache cache) {
    String TEST = "test";
    AuthAdc authAdc = TestUtils.prepareAuthAdc(TEST);
    CloudConf cloudConf = new CloudConf(TEST, TEST, TEST, TEST);
    FuseDao fuseDao = new FuseDaoImpl(authAdc, httpClientFactory);
    Arguments arguments = new Arguments();
    arguments.cloudConf = cloudConf;
    Parameters parameters = new Parameters(fuseDao, arguments,
        Platform.getNativePlatform().getOS());
    DicomPathCacher dicomPathCacher = new DicomPathCacher();
    return new DicomFuseHelper(parameters, dicomPathCacher, cache);
  }
}