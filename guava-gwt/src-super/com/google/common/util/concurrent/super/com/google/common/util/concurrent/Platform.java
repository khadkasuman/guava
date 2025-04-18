/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;

/** Methods factored out so that they can be emulated differently in GWT. */
final class Platform {
  static boolean isInstanceOfThrowableClass(Throwable t, Class<? extends Throwable> expectedClass) {
    /*
     * This method is used only by CatchingFuture, and CatchingFuture accepts only Throwable.class
     * under GWT.
     */
    return true;
  }

  static void restoreInterruptIfIsInterruptedException(Throwable t) {}

  static void interruptCurrentThread() {}

  static void rethrowIfErrorOtherThanStackOverflow(Throwable t) {
    if (t instanceof Error) {
      // There is no StackOverflowError under GWT/J2CL.
      throw (Error) t;
    }
  }

  static <V extends @Nullable Object> V get(AbstractFuture<V> future)
      throws InterruptedException, ExecutionException {
    return future.getFromAlreadyDoneTrustedFuture();
  }

  static <V extends @Nullable Object> V get(AbstractFuture<V> future, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    checkNotNull(unit);
    return future.getFromAlreadyDoneTrustedFuture();
  }

  private Platform() {}
}
