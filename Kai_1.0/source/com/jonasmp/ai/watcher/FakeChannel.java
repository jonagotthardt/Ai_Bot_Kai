package com.jonasmp.ai.watcher;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultChannelProgressivePromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

public class FakeChannel extends AbstractChannel {
   private static final ChannelMetadata METADATA = new ChannelMetadata(false);
   private final SocketAddress localAddress;
   private final SocketAddress remoteAddress;
   private final ChannelConfig config;
   private final EventLoop eventLoop;
   private final ChannelPipeline pipeline;

   public FakeChannel(SocketAddress localAddress, SocketAddress remoteAddress) {
      super((Channel)null);
      this.localAddress = localAddress;
      this.remoteAddress = remoteAddress;
      this.config = new DefaultChannelConfig(this);
      this.eventLoop = new DefaultEventLoop();
      this.pipeline = new FakeChannel.FakeChannelPipeline(this);
   }

   public EventLoop eventLoop() {
      return this.eventLoop;
   }

   public boolean isRegistered() {
      return true;
   }

   public ChannelConfig config() {
      return this.config;
   }

   public ChannelPipeline pipeline() {
      return this.pipeline;
   }

   public boolean isCompatible(EventLoop loop) {
      return true;
   }

   public ChannelMetadata metadata() {
      return METADATA;
   }

   protected void doRegister() throws Exception {
   }

   protected void doBind(SocketAddress localAddress) throws Exception {
   }

   protected void doDisconnect() throws Exception {
   }

   protected void doClose() throws Exception {
      this.eventLoop.shutdownGracefully();
   }

   protected void doBeginRead() throws Exception {
   }

   protected void doWrite(ChannelOutboundBuffer in) throws Exception {
      while (true) {
         Object msg = in.current();
         if (msg == null) {
            return;
         }

         ReferenceCountUtil.release(msg);
         in.remove();
      }
   }

   public boolean isActive() {
      return true;
   }

   public boolean isOpen() {
      return true;
   }

   protected SocketAddress localAddress0() {
      return this.localAddress;
   }

   protected SocketAddress remoteAddress0() {
      return this.remoteAddress;
   }

   protected AbstractUnsafe newUnsafe() {
      return new FakeChannel.FakeUnsafe();
   }

   private static class FakeChannelPipeline implements ChannelPipeline {
      private final Channel channel;
      private final ChannelHandlerContext head = new FakeChannel.FakeContext(this, "_head_");
      private final ChannelHandlerContext tail = new FakeChannel.FakeContext(this, "_tail_");

      FakeChannelPipeline(Channel channel) {
         this.channel = channel;
      }

      public Channel channel() {
         return this.channel;
      }

      public ChannelPipeline addFirst(String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addFirst(ChannelHandler... handlers) {
         return this;
      }

      public ChannelPipeline addLast(String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addLast(ChannelHandler... handlers) {
         return this;
      }

      public ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers) {
         return this;
      }

      public ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers) {
         return this;
      }

      public ChannelPipeline remove(ChannelHandler handler) {
         return this;
      }

      public ChannelHandler remove(String name) {
         return null;
      }

      public <T extends ChannelHandler> T remove(Class<T> handlerType) {
         return null;
      }

      public ChannelHandler removeFirst() {
         return null;
      }

      public ChannelHandler removeLast() {
         return null;
      }

      public ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
         return null;
      }

      public ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
         return this;
      }

      public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler) {
         return null;
      }

      public ChannelHandler first() {
         return null;
      }

      public ChannelHandler last() {
         return null;
      }

      public ChannelHandlerContext firstContext() {
         return this.head;
      }

      public ChannelHandlerContext lastContext() {
         return this.tail;
      }

      public ChannelHandler get(String name) {
         return null;
      }

      public <T extends ChannelHandler> T get(Class<T> handlerType) {
         return null;
      }

      public ChannelHandlerContext context(ChannelHandler handler) {
         return handler == null ? null : new FakeChannel.FakeContext(this, "stub");
      }

      public ChannelHandlerContext context(String name) {
         if ("_head_".equals(name)) {
            return this.head;
         } else {
            return (ChannelHandlerContext)("_tail_".equals(name) ? this.tail : new FakeChannel.FakeContext(this, name));
         }
      }

      public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
         return new FakeChannel.FakeContext(this, "stub");
      }

      public List<String> names() {
         return Collections.emptyList();
      }

      public Map<String, ChannelHandler> toMap() {
         return Collections.emptyMap();
      }

      public Iterator<Entry<String, ChannelHandler>> iterator() {
         return Collections.emptyIterator();
      }

      public ChannelPipeline fireChannelRegistered() {
         return this;
      }

      public ChannelPipeline fireChannelUnregistered() {
         return this;
      }

      public ChannelPipeline fireChannelActive() {
         return this;
      }

      public ChannelPipeline fireChannelInactive() {
         return this;
      }

      public ChannelPipeline fireExceptionCaught(Throwable cause) {
         return this;
      }

      public ChannelPipeline fireUserEventTriggered(Object event) {
         return this;
      }

      public ChannelPipeline fireChannelRead(Object msg) {
         ReferenceCountUtil.release(msg);
         return this;
      }

      public ChannelPipeline fireChannelReadComplete() {
         return this;
      }

      public ChannelPipeline fireChannelWritabilityChanged() {
         return this;
      }

      public ChannelFuture bind(SocketAddress localAddress) {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture connect(SocketAddress remoteAddress) {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture disconnect() {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture close() {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture deregister() {
         return this.newFailedFuture(new UnsupportedOperationException());
      }

      public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture disconnect(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture close(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture deregister(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelPipeline read() {
         return this;
      }

      public ChannelFuture write(Object msg) {
         ReferenceCountUtil.release(msg);
         return this.newSucceededFuture();
      }

      public ChannelFuture write(Object msg, ChannelPromise promise) {
         ReferenceCountUtil.release(msg);
         promise.setSuccess();
         return promise;
      }

      public ChannelPipeline flush() {
         return this;
      }

      public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
         ReferenceCountUtil.release(msg);
         promise.setSuccess();
         return promise;
      }

      public ChannelFuture writeAndFlush(Object msg) {
         ReferenceCountUtil.release(msg);
         return this.newSucceededFuture();
      }

      public ChannelPromise newPromise() {
         return new DefaultChannelPromise(this.channel, this.channel.eventLoop());
      }

      public ChannelProgressivePromise newProgressivePromise() {
         return new DefaultChannelProgressivePromise(this.channel, this.channel.eventLoop());
      }

      public ChannelFuture newSucceededFuture() {
         ChannelPromise p = new DefaultChannelPromise(this.channel, this.channel.eventLoop());
         p.setSuccess();
         return p;
      }

      public ChannelFuture newFailedFuture(Throwable cause) {
         ChannelPromise p = new DefaultChannelPromise(this.channel, this.channel.eventLoop());
         p.setFailure(cause);
         return p;
      }

      public ChannelPromise voidPromise() {
         return new DefaultChannelPromise(this.channel, this.channel.eventLoop());
      }

      public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
         return this;
      }

      public ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
         return this;
      }
   }

   private static class FakeContext implements ChannelHandlerContext {
      private final ChannelPipeline pipeline;
      private final String name;

      FakeContext(ChannelPipeline pipeline, String name) {
         this.pipeline = pipeline;
         this.name = name;
      }

      public ChannelPipeline pipeline() {
         return this.pipeline;
      }

      public Channel channel() {
         return this.pipeline.channel();
      }

      public EventExecutor executor() {
         return this.channel().eventLoop();
      }

      public String name() {
         return this.name;
      }

      public ChannelHandler handler() {
         return null;
      }

      public boolean isRemoved() {
         return false;
      }

      public ChannelHandlerContext fireChannelRegistered() {
         return this;
      }

      public ChannelHandlerContext fireChannelUnregistered() {
         return this;
      }

      public ChannelHandlerContext fireChannelActive() {
         return this;
      }

      public ChannelHandlerContext fireChannelInactive() {
         return this;
      }

      public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
         return this;
      }

      public ChannelHandlerContext fireUserEventTriggered(Object event) {
         return this;
      }

      public ChannelHandlerContext fireChannelRead(Object msg) {
         ReferenceCountUtil.release(msg);
         return this;
      }

      public ChannelHandlerContext fireChannelReadComplete() {
         return this;
      }

      public ChannelHandlerContext fireChannelWritabilityChanged() {
         return this;
      }

      public ChannelHandlerContext read() {
         return this;
      }

      public ChannelHandlerContext flush() {
         return this;
      }

      public ChannelFuture write(Object msg) {
         ReferenceCountUtil.release(msg);
         return this.newFailedFuture();
      }

      public ChannelFuture write(Object msg, ChannelPromise promise) {
         ReferenceCountUtil.release(msg);
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture writeAndFlush(Object msg) {
         ReferenceCountUtil.release(msg);
         return this.newFailedFuture();
      }

      public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
         ReferenceCountUtil.release(msg);
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelPromise newPromise() {
         return new DefaultChannelPromise(this.channel(), this.channel().eventLoop());
      }

      public ChannelProgressivePromise newProgressivePromise() {
         return new DefaultChannelProgressivePromise(this.channel(), this.channel().eventLoop());
      }

      public ChannelFuture newSucceededFuture() {
         ChannelPromise p = new DefaultChannelPromise(this.channel(), this.channel().eventLoop());
         p.setSuccess();
         return p;
      }

      public ChannelFuture newFailedFuture(Throwable cause) {
         ChannelPromise p = new DefaultChannelPromise(this.channel(), this.channel().eventLoop());
         p.setFailure(cause);
         return p;
      }

      public ChannelPromise voidPromise() {
         return new DefaultChannelPromise(this.channel(), this.channel().eventLoop());
      }

      public ChannelFuture bind(SocketAddress localAddress) {
         return this.newFailedFuture();
      }

      public ChannelFuture connect(SocketAddress remoteAddress) {
         return this.newFailedFuture();
      }

      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
         return this.newFailedFuture();
      }

      public ChannelFuture disconnect() {
         return this.newFailedFuture();
      }

      public ChannelFuture close() {
         return this.newFailedFuture();
      }

      public ChannelFuture deregister() {
         return this.newFailedFuture();
      }

      public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture disconnect(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture close(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ChannelFuture deregister(ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException());
         return promise;
      }

      public ByteBufAllocator alloc() {
         return this.channel().alloc();
      }

      public <T> Attribute<T> attr(AttributeKey<T> key) {
         return this.channel().attr(key);
      }

      public <T> boolean hasAttr(AttributeKey<T> key) {
         return this.channel().hasAttr(key);
      }

      private ChannelFuture newFailedFuture() {
         ChannelPromise p = new DefaultChannelPromise(this.channel(), this.channel().eventLoop());
         p.setFailure(new UnsupportedOperationException());
         return p;
      }
   }

   private class FakeUnsafe extends AbstractUnsafe {
      private FakeUnsafe() {
         Objects.requireNonNull(FakeChannel.this);
         super(FakeChannel.this);
      }

      public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
         promise.setSuccess();
      }
   }
}
