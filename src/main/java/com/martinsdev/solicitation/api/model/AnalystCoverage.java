package com.martinsdev.solicitation.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "tb_analyst_coverage")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class AnalystCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(unique = true, name = "user_id", nullable = false)
    private User user; // id do analyst

    @ElementCollection // criacao de uma coleção de tipo simples, o hibernate cria uma tabela filha separada
    @CollectionTable(name = "tb_analyst_coverage_states", joinColumns = @JoinColumn(name = "analyst_coverage_id")) // evita o comportamento padrao do hibernate
    @Column(length = 2)
    private Set<String> states;
}
