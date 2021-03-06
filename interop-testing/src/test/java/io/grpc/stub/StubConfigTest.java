/*
 * Copyright 2014, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc.stub;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import io.grpc.testing.integration.Messages.SimpleRequest;
import io.grpc.testing.integration.Messages.SimpleResponse;
import io.grpc.testing.integration.TestServiceGrpc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for stub reconfiguration.
 */
@RunWith(JUnit4.class)
public class StubConfigTest {

  @Mock
  private Channel channel;

  @Mock
  private StreamObserver<SimpleResponse> responseObserver;

  @Mock
  private ClientCall<SimpleRequest, SimpleResponse> call;

  /**
   * Sets up mocks.
   */
  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(channel.newCall(
      Mockito.<MethodDescriptor<SimpleRequest, SimpleResponse>>any(), any(CallOptions.class)))
      .thenReturn(call);
  }

  @Test
  public void testConfigureDeadline() {
    Deadline deadline = Deadline.after(2, NANOSECONDS);
    // Create a default stub
    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(channel);
    assertNull(stub.getCallOptions().getDeadline());
    // Reconfigure it
    TestServiceGrpc.TestServiceBlockingStub reconfiguredStub = stub.withDeadline(deadline);
    // New altered config
    assertEquals(deadline, reconfiguredStub.getCallOptions().getDeadline());
    // Default config unchanged
    assertNull(stub.getCallOptions().getDeadline());
  }

  @Test
  @Deprecated
  public void testConfigureDeadlineNanoTime() {
    long deadline = System.nanoTime() + SECONDS.toNanos(1);
    // Create a default stub
    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(channel);
    assertNull(stub.getCallOptions().getDeadlineNanoTime());
    // Reconfigure it
    TestServiceGrpc.TestServiceBlockingStub reconfiguredStub = stub.withDeadlineNanoTime(deadline);
    // New altered config
    assertNotNull(reconfiguredStub.getCallOptions().getDeadlineNanoTime());
    long maxDelta = MILLISECONDS.toNanos(20);
    long actualDelta = Math.abs(reconfiguredStub.getCallOptions().getDeadlineNanoTime() - deadline);
    assertTrue(maxDelta + " < " + actualDelta, maxDelta >= actualDelta);
    // Default config unchanged
    assertNull(stub.getCallOptions().getDeadlineNanoTime());
  }

  @Test
  public void testStubCallOptionsPopulatedToNewCall() {
    TestServiceGrpc.TestServiceStub stub = TestServiceGrpc.newStub(channel);
    CallOptions options1 = stub.getCallOptions();
    SimpleRequest request = SimpleRequest.getDefaultInstance();
    stub.unaryCall(request, responseObserver);
    verify(channel).newCall(same(TestServiceGrpc.METHOD_UNARY_CALL), same(options1));
    stub = stub.withDeadlineAfter(2, NANOSECONDS);
    CallOptions options2 = stub.getCallOptions();
    assertNotSame(options1, options2);
    stub.unaryCall(request, responseObserver);
    verify(channel).newCall(same(TestServiceGrpc.METHOD_UNARY_CALL), same(options2));
  }
}
