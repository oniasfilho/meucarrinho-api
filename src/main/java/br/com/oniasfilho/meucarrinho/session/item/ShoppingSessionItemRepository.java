package br.com.oniasfilho.meucarrinho.session.item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingSessionItemRepository extends JpaRepository<ShoppingSessionItem, UUID> {

    @Query("""
        select i from ShoppingSessionItem i
        where i.session.id = :sessionId
        order by i.createdAt desc
        """)
    List<ShoppingSessionItem> findAllBySessionIdOrderByCreatedAtDesc(@Param("sessionId") UUID sessionId);

    @Query("""
        select i from ShoppingSessionItem i
        where i.session.id in :sessionIds
        order by i.createdAt desc
        """)
    List<ShoppingSessionItem> findAllBySessionIdInOrderByCreatedAtDesc(
        @Param("sessionIds") Collection<UUID> sessionIds
    );

    @Query("select i from ShoppingSessionItem i where i.id = :id and i.session.id = :sessionId")
    Optional<ShoppingSessionItem> findByIdAndSessionId(
        @Param("id") UUID id,
        @Param("sessionId") UUID sessionId
    );

    @Query("""
        select i from ShoppingSessionItem i
        where i.session.user.id = :userId and i.session.status = :status
        """)
    List<ShoppingSessionItem> findAllByUserIdAndSessionStatus(
        @Param("userId") UUID userId,
        @Param("status") ShoppingSessionStatus status
    );
}
