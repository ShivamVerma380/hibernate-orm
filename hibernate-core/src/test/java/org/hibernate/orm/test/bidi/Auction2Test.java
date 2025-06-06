/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bidi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsExistsInSelectCheck.class,
		comment = "dialect does not support exist predicates in the select clause"
)
@DomainModel(xmlMappings = "org/hibernate/orm/test/bidi/Auction2.xml")
@SessionFactory
public class Auction2Test {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testLazy(SessionFactoryScope scope) {
		Auction auction = new Auction();
		auction.setDescription( "an auction for something" );
		auction.setEnd( new Date() );
		Bid bid = new Bid();
		bid.setAmount( new BigDecimal( "123.34" ).setScale( 19, RoundingMode.DOWN ) );
		bid.setSuccessful( true );
		bid.setDatetime( new Date() );
		bid.setItem( auction );
		auction.getBids().add( bid );
		auction.setSuccessfulBid( bid );
		scope.inTransaction(
				session ->
						session.persist( bid )
		);

		Long aid = auction.getId();
		Long bidId = bid.getId();

		scope.inTransaction(
				session -> {
					Bid b = session.getReference( Bid.class, bidId );
					assertFalse( Hibernate.isInitialized( b ) );
					Auction a = session.find( Auction.class, aid );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertFalse( Hibernate.isInitialized( a.getSuccessfulBid() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertSame( b, a.getSuccessfulBid() );
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( b.isSuccessful() );
				}
		);

		scope.inTransaction(
				session -> {
					Bid b = session.getReference( Bid.class, bidId );
					assertFalse( Hibernate.isInitialized( b ) );
					Auction a = session.createQuery( "from Auction a left join fetch a.bids", Auction.class ).uniqueResult();
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( Hibernate.isInitialized( a.getBids() ) );
					assertSame( b, a.getSuccessfulBid() );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);

		scope.inTransaction(
				session -> {
					Bid b = session.getReference( Bid.class, bidId );
					Auction a = session.getReference( Auction.class, aid );
					assertFalse( Hibernate.isInitialized( b ) );
					assertFalse( Hibernate.isInitialized( a ) );
					session.createQuery( "from Auction a left join fetch a.successfulBid", Auction.class ).list();
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( Hibernate.isInitialized( a ) );
					assertSame( b, a.getSuccessfulBid() );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);

		scope.inTransaction(
				session -> {
					Bid b = session.getReference( Bid.class, bidId );
					Auction a = session.getReference( Auction.class, aid );
					assertFalse( Hibernate.isInitialized( b ) );
					assertFalse( Hibernate.isInitialized( a ) );
					assertSame( session.find( Bid.class, bidId ), b );
					assertTrue( Hibernate.isInitialized( b ) );
					assertSame( session.find( Auction.class, aid ), a );
					assertTrue( Hibernate.isInitialized( a ) );
					assertSame( b, a.getSuccessfulBid() );
					assertSame( a, a.getSuccessfulBid().getItem() );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);
	}

}
