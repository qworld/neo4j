/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api;

import org.junit.Test;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.store.NeoStore;
import org.neo4j.kernel.impl.transaction.TransactionHeaderInformationFactory;
import org.neo4j.kernel.impl.transaction.TransactionMonitor;
import org.neo4j.kernel.impl.transaction.state.NeoStoreTransactionContext;
import org.neo4j.kernel.impl.transaction.state.NeoStoreTransactionContextSupplier;
import org.neo4j.kernel.lifecycle.LifeSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.neo4j.helpers.collection.IteratorUtil.asSet;
import static org.neo4j.helpers.collection.IteratorUtil.asUniqueSet;

public class KernelTransactionsTest
{
    @Test
    public void shouldListActiveTransactions() throws Exception
    {
        // Given
        LifeSupport life = new LifeSupport();
        life.start();

        Locks locks = mock( Locks.class );
        when(locks.newClient()).thenReturn( mock( Locks.Client.class ) );

        KernelTransactions registry = new KernelTransactions(
                new MockContextSupplier(), mock(NeoStore.class), locks, null, null, null, null, null, null,
                null, null, TransactionHeaderInformationFactory.DEFAULT, null, null,  mock(TransactionCommitProcess.class), null, null,
                new TransactionHooks(), mock( TransactionMonitor.class ), life, false );

        // When
        KernelTransaction first  = registry.newInstance();
        KernelTransaction second = registry.newInstance();
        KernelTransaction third  = registry.newInstance();

        first.close();

        // Then
        assertThat( asUniqueSet(registry.activeTransactions()), equalTo(asSet( second, third )) );
    }

    @Test
    public void shouldDisposeTransactionsWhenAsked() throws Exception
    {
        // Given
        LifeSupport life = new LifeSupport();
        life.start();

        Locks locks = mock( Locks.class );
        when(locks.newClient()).thenReturn( mock( Locks.Client.class ) );

        KernelTransactions registry = new KernelTransactions(
                new MockContextSupplier(), mock(NeoStore.class), locks, null, null, null, null, null, null,
                null, null, TransactionHeaderInformationFactory.DEFAULT, null, null,  mock(TransactionCommitProcess.class), null, null,
                new TransactionHooks(), mock( TransactionMonitor.class ), life, false );

        registry.disposeAll();

        KernelTransaction first  = registry.newInstance();
        KernelTransaction second  = registry.newInstance();
        KernelTransaction leftOpen  = registry.newInstance();
        first.close();
        second.close();

        // When
        registry.disposeAll();

        // Then
        KernelTransaction postDispose = registry.newInstance();
        assertThat( postDispose, not( equalTo( first ) ) );
        assertThat( postDispose, not( equalTo( second ) ) );

        assertTrue(leftOpen.shouldBeTerminated());
    }

    private static class MockContextSupplier extends NeoStoreTransactionContextSupplier
    {
        public MockContextSupplier()
        {
            super( null );
        }

        @Override
        protected NeoStoreTransactionContext create()
        {
            return mock(NeoStoreTransactionContext.class);
        }
    }
}
