/*
 * Copyright 2017-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.streams.sources;

import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.motion.observable.IndefiniteObservable;
import com.google.android.material.motion.streams.MotionObservable;
import com.google.android.material.motion.streams.gestures.OnTouchListeners;
import com.google.android.material.motion.streams.interactions.Tap;
import com.google.android.material.motion.streams.operators.CommonOperators;

public class TapSource {

  public static MotionObservable<Float[]> from(final Tap tap) {
    return new MotionObservable<>(new IndefiniteObservable.Connector<MotionObservable.MotionObserver<Float[]>>() {

      private View container;
      private GestureDetectorCompat detector;

      @NonNull
      @Override
      public IndefiniteObservable.Disconnector connect(
        final MotionObservable.MotionObserver<Float[]> observer) {
        container = tap.container;
        detector = new GestureDetectorCompat(
          container.getContext(),
          new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
              observer.next(new Float[]{e.getX(), e.getY()});
              return true;
            }
          });
        detector.setOnDoubleTapListener(null);
        detector.setIsLongpressEnabled(false);

        final IndefiniteObservable.Subscription enabledSubscription =
          tap.enabled.getStream()
            .compose(CommonOperators.<Boolean>dedupe())
            .subscribe(new MotionObservable.SimpleMotionObserver<Boolean>() {
              @Override
              public void next(Boolean value) {
                if (value) {
                  start();
                } else {
                  stop();
                }
              }
            });

        return new IndefiniteObservable.Disconnector() {
          @Override
          public void disconnect() {
            enabledSubscription.unsubscribe();
            stop();
          }
        };
      }

      private void start() {
        OnTouchListeners.add(container, listener);
      }

      private void stop() {
        OnTouchListeners.remove(container, listener);
      }

      private final View.OnTouchListener listener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          detector.onTouchEvent(event);
          return true;
        }
      };
    });
  }
}
