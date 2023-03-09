package com.ziqni.admin.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ZiqniLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private static final Logger logger = LoggerFactory.getLogger(ZiqniLinkedBlockingQueue.class);

    private final AtomicLong sizeAtomicLong = new AtomicLong();
    private final String name;
    private final int highWaterMark;

    public ZiqniLinkedBlockingQueue(String name, int highWaterMark) {
        super();
        this.name=name;
        this.highWaterMark=highWaterMark;
    }

    public ZiqniLinkedBlockingQueue(String name, int highWaterMark, int capacity) {
        super(capacity);
        this.name=name;
        this.highWaterMark=highWaterMark;
    }

    public ZiqniLinkedBlockingQueue(String name, int highWaterMark, Collection<? extends E> c) {
        super(c);
        this.name=name;
        this.highWaterMark=highWaterMark;
    }

    private void alert(Long currentCount){
        if(currentCount < -10L){
            logger.error("Queue {} counter is out of sync, re-syncing {}/{} ",name,currentCount,size());
            sizeAtomicLong.set(size());
        }
        else if(currentCount > highWaterMark){
            logger.warn("Queue {} has exceeded the high water mark {}/{}",name,currentCount,highWaterMark);
        }
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public int remainingCapacity() {
        return super.remainingCapacity();
    }

    @Override
    public void put(E e) throws InterruptedException {
        sizeAtomicLong.incrementAndGet();
        super.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        sizeAtomicLong.incrementAndGet();
        return super.offer(e, timeout, unit);
    }

    @Override
    public boolean offer(E e) {
        sizeAtomicLong.incrementAndGet();
        return super.offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        sizeAtomicLong.decrementAndGet();
        return super.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return super.poll(timeout, unit);
    }

    @Override
    public E poll() {
        return super.poll();
    }

    @Override
    public E peek() {
        return super.peek();
    }

    @Override
    public boolean remove(Object o) {
        sizeAtomicLong.decrementAndGet();
        return super.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return super.toArray(a);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void clear() {
        sizeAtomicLong.set(0);
        super.clear();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return super.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        final var out = super.drainTo(c, maxElements);
        sizeAtomicLong.set(this.size());
        return out;
    }

    @Override
    public Iterator<E> iterator() {
        return super.iterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return super.spliterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        super.forEach(action);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return super.removeIf(filter);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public boolean add(E e) {
        return super.add(e);
    }

    @Override
    public E remove() {
        return super.remove();
    }

    @Override
    public E element() {
        return super.element();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return super.addAll(c);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return super.toArray(generator);
    }

    @Override
    public Stream<E> stream() {
        return super.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return super.parallelStream();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
