//
// JSCTest.java
//
// LiquidPlayer project
// https://github.com/LiquidPlayer
//
// Created by Eric Lange
//
/*
 Copyright (c) 2016 Eric Lange. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.liquidplayer.test;

import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.liquidplayer.javascript.JSContext;
import org.liquidplayer.javascript.JSContextGroup;
import org.liquidplayer.node.Process;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.*;

public class JSCTest {

    private class InNodeProcess implements Process.EventListener {

        final Semaphore processCompleted = new Semaphore(0);
        private final Runnable runnable;

        InNodeProcess(final Runnable runnable, String vm) {
            this.runnable = runnable;
            new Process(InstrumentationRegistry.getContext(),vm,
                    Process.kMediaAccessPermissionsRW,this);
        }

        @Override
        public void onProcessStart(final Process proc, final JSContext ctx) {
            group = ctx.getGroup();
            context = ctx;
            process = proc;
            runnable.run();
        }

        @Override
        public void onProcessAboutToExit(Process process, int exitCode) {
        }

        @Override
        public void onProcessExit(Process process, int exitCode) {
            processCompleted.release();
        }

        @Override
        public void onProcessFailed(Process process, Exception error) {

        }
    }

    private Exception exception = null;
    private JSContextGroup group = null;
    private JSContext context = null;
    private Process process = null;
    @Before
    public void setUp() {
        exception = null;
        group = null;
        context = null;
        process = null;
    }

    @Test
    public void testJavaScriptCoreBridge() throws Exception {
        JSC jsc = new JSC(null);
        assertEquals(0,jsc.testAPI());
    }

    @Test
    public void testJavaScriptCoreMiniDOM() throws Exception {
        JSC jsc = new JSC(null);
        assertEquals(0,jsc.testMinidom());
    }

    @Test
    public void testJavaScriptCoreBridgeInNode() throws Exception {
        (new InNodeProcess(new Runnable() {
            @Override
            public void run() {
                try {
                    JSC jsc = new JSC(group);
                    assertEquals(0, jsc.testAPI());
                } catch (Exception e) {
                    exception = e;
                }
            }
        }, "_testapi")).processCompleted.acquire();

        if (exception != null) throw exception;
    }

    @Test
    public void testJavaScriptCoreBridgeOutsideNode() throws Exception {
        final Semaphore ready = new Semaphore(0);
        InNodeProcess inp = new InNodeProcess(new Runnable() {
            @Override
            public void run() {
                process.keepAlive();
                ready.release();
            }
        }, "_testapi");
        // Wait until process is active
        ready.acquire();

        // Test outside of node thread
        JSC jsc = new JSC(group);
        assertEquals(0, jsc.testAPI());

        process.letDie();
        inp.processCompleted.acquire();
    }

    @Test
    public void testJavaScriptCoreMiniDOMInNode() throws Exception {
        (new InNodeProcess(new Runnable() {
            @Override
            public void run() {
                try {
                    JSC jsc = new JSC(group);
                    assertEquals(0, jsc.testMinidom());
                } catch (Exception e) {
                    exception = e;
                }
            }
        }, "_testapi")).processCompleted.acquire();

        if (exception != null) throw exception;
    }

    @Test
    public void testJavaScriptCoreMiniDomOutsideNode() throws Exception {
        final Semaphore ready = new Semaphore(0);
        InNodeProcess inp = new InNodeProcess(new Runnable() {
            @Override
            public void run() {
                process.keepAlive();
                ready.release();
            }
        }, "_testapi");
        // Wait until process is active
        ready.acquire();

        // Test outside of node thread
        JSC jsc = new JSC(group);
        assertEquals(0, jsc.testMinidom());

        process.letDie();
        inp.processCompleted.acquire();
    }

}