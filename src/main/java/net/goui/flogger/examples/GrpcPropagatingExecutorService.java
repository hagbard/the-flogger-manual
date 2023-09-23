/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Copyright (c) 2023, David Beaumont (https://github.com/hagbard).

This program and the accompanying materials are made available under the terms of the
Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package net.goui.flogger.examples;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * An executor service wraps a delegated service to propagate the current gRPC context into the
 * generated task. For convenience, it also wraps the delegate as a {@link
 * ListeningExecutorService}.
 */
public final class GrpcPropagatingExecutorService extends AbstractExecutorService {
  public static ListeningExecutorService wrap(ExecutorService delegate) {
    return MoreExecutors.listeningDecorator(new GrpcPropagatingExecutorService(delegate));
  }

  private static final class GrpcPropagatingTask<V> extends FutureTask<V> {
    public GrpcPropagatingTask(Callable<V> callable) {
      super(Context.current().wrap(callable));
    }

    public GrpcPropagatingTask(Runnable runnable, V result) {
      super(Context.current().wrap(runnable), result);
    }
  }

  private final ExecutorService delegate;

  private GrpcPropagatingExecutorService(ExecutorService delegate) {
    this.delegate = delegate;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new GrpcPropagatingTask<>(runnable, value);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new GrpcPropagatingTask<>(callable);
  }

  @Override
  public void execute(Runnable command) {
    if (!(command instanceof GrpcPropagatingTask)) {
      command = Context.current().wrap(command);
    }
    delegate.execute(command);
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }
}
