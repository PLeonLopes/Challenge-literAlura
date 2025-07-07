package com.alura.literAlura.repository;

import com.alura.literAlura.model.Autor;
import com.alura.literAlura.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Year;
import java.util.List;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    // Query Livros por Título
    @Query("SELECT li FROM Livro li WHERE LOWER(li.titulo) = LOWER(:titulo)")
    List<Livro> findByTitulo(String titulo);

    // Query Autores Vivos
    @Query("SELECT au FROM Autor au WHERE au.anoNascimento <= :ano AND (au.anoFalecimento IS NULL OR au.anoFalecimento >= :ano)")
    List<Autor> findAutoresVivos(@Param("ano") Year ano);

    // Query Autores Vivos 2
    @Query("SELECT au FROM Autor au WHERE au.anoNascimento = :ano AND (au.anoFalecimento IS NULL OR au.anoFalecimento >= :ano)")
    List<Autor> findAutoresVivosRefinado(@Param("ano") Year ano);

    // Query Autores por Ano de morte
    @Query("SELECT au FROM Autor au WHERE au.anoNascimento <= :ano AND au.anoFalecimento = :ano")
    List<Autor> findAutoresPorAnoDeMorte(@Param("ano") Year ano);

    // Query Livros por Idioma
    @Query("SELECT li FROM Livro li WHERE li.idioma LIKE %:idioma%")
    List<Livro> findByIdioma(@Param("idioma") String idioma);
}
