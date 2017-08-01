package com.netflix.ndbench.core;

import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.test.Archaius2TestConfig;
import com.netflix.archaius.test.TestPropertyOverride;
import com.netflix.governator.guice.test.ModulesForTesting;
import com.netflix.governator.guice.test.ReplaceWithMock;
import com.netflix.governator.guice.test.junit4.GovernatorJunit4ClassRunner;
import com.netflix.ndbench.api.plugin.DataGenerator;
import com.netflix.ndbench.api.plugin.NdBenchClient;
import com.netflix.ndbench.api.plugin.NdBenchMonitor;
import com.netflix.ndbench.api.plugin.common.NdBenchConstants;
import com.netflix.ndbench.core.config.IConfiguration;
import com.netflix.ndbench.core.defaultimpl.NdBenchGuiceModule;
import com.netflix.ndbench.core.operations.WriteOperation;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.*;

@RunWith(GovernatorJunit4ClassRunner.class)
@ModulesForTesting({NdBenchGuiceModule.class, ArchaiusModule.class})

public class NdbenchDriverTest {
    @Rule
    @RuntimeLayer
    public Archaius2TestConfig settableConfig = new Archaius2TestConfig();

    @Inject
    IConfiguration config;

    @Inject
    NdBenchMonitor ndBenchMonitor;

    @Inject
    DataGenerator dataGenerator;



    @Test
    public void testInvokingProcessMethodOnWriteOperationSetsNewRateLimit() throws Exception {
        NdBenchClient mockClientPlugin = mock(NdBenchClient.class);
        when(mockClientPlugin.writeSingle(anyString())).thenReturn("foo");
        when(mockClientPlugin.
                autoTuneWriteRateLimit(anyDouble(), anyString(), any(NdBenchMonitor.class))).
                thenReturn(500D);

        NdBenchMonitor  mockMonitor = mock(NdBenchMonitor .class);
        doNothing().when(mockMonitor).recordReadLatency(anyLong());
        doNothing().when(mockMonitor).incWriteSuccess();

        when(mockClientPlugin.writeSingle(anyString())).thenReturn("foo");

        NdBenchDriver driver = new NdBenchDriver(config, ndBenchMonitor, dataGenerator, settableConfig);
        WriteOperation writeOperation = new WriteOperation(mockClientPlugin);

        writeOperation.
                process(driver, mockMonitor, "some-key", new AtomicReference<>(RateLimiter.create(100)), true);

        int rateFromSettableConfig = settableConfig.getInteger(NdBenchConstants.WRITE_RATE_LIMIT_FULL_NAME);


        assertEquals(rateFromSettableConfig , 500D, .001);

        // Next check won't work unless we figure out how to configure Property Listener to kick in during the test run
        //double rateFromDriverRateLimiter = driver.getWriteLimiter().get().getRate();
        //assertEquals(rateFromDriverRateLimiter, 500D, .001);
    }
}