/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.ListIterator;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@code ForwardingListIterator}.
 *
 * @author Robert Konigsberg
 */
@NullUnmarked
public class ForwardingListIteratorTest extends TestCase {

  @SuppressWarnings("rawtypes")
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            ListIterator.class,
            new Function<ListIterator, ListIterator<?>>() {
              @Override
              public ListIterator<?> apply(ListIterator delegate) {
                return wrap((ListIterator<?>) delegate);
              }
            });
  }

  private static <T> ListIterator<T> wrap(ListIterator<T> delegate) {
    return new ForwardingListIterator<T>() {
      @Override
      protected ListIterator<T> delegate() {
        return delegate;
      }
    };
  }
}
