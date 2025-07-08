package com.alura.literAlura.principal;

import com.alura.literAlura.model.Autor;
import com.alura.literAlura.model.AutorDTO;
import com.alura.literAlura.model.Livro;
import com.alura.literAlura.model.LivroDTO;
import com.alura.literAlura.repository.LivroRepository;
import com.alura.literAlura.service.ConsumoAPI;
import com.alura.literAlura.service.ConverteDados;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Principal {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConverteDados converteDados;

    private final Scanner entrada = new Scanner(System.in);

    public Principal(LivroRepository livroRepository, ConsumoAPI consumoAPI, ConverteDados converteDados) {
        this.livroRepository = livroRepository;
        this.consumoAPI = consumoAPI;
        this.converteDados = converteDados;
    }

    public void exec() {
        boolean isActive = true;
        while (isActive) {
            menu();
            var opcao = entrada.nextInt();
            entrada.nextLine();                     // limpa buffer

            switch (opcao) {
                case 1 -> buscaLivrosPeloTitulo();
                case 2 -> listaLivrosRegistrados();
                case 3 -> listaAutoresRegistrados();
                case 4 -> listaAutoresVivos();
                case 5 -> listaAutoresVivosRefinado();
                case 6 -> listaAutoresPorAnoDeMorte();
                case 7 -> listaLivrosPorIdioma();
                case 0 -> {
                    System.out.println("Programa Encerrado. Obrigado por utilizar!");
                    isActive = false;
                }
                default -> System.out.println("Opção inválida!");
            }
        }
    }

    private void menu() {
        System.out.println("""
                                 Menu
                       1- Buscar livros pelo título
                       2- Listar livros registrados
                       3- Listar autores registrados
                       4- Listar autores vivos em um determinado ano
                       5- Listar autores nascidos em determinado ano
                       6- Listar autores por ano de sua morte
                       7- Listar livros em um determinado idioma
                       0- Sair
            """);
    }

    private void salvarLivros(List<Livro> livros) {
        livros.forEach(livroRepository::save);
    }

    private void buscaLivrosPeloTitulo() {
        String baseURL = "https://gutendex.com/books?search=";          // URL API (gutendex)

        try {
            System.out.println("Digite o título do livro: ");
            String titulo = entrada.nextLine();
            String endereco = baseURL + titulo.replace(" ", "%20");
            System.out.println("URL da API: " + endereco);

            String jsonResponse = consumoAPI.obterDados(endereco);
            System.out.println("Resposta da API: " + jsonResponse);

            if (jsonResponse.isEmpty()) {
                System.out.println("A resposta da API está vazia.");
                return;
            }

            // Extrai lista de livros de "results"
            JsonNode rootNode = converteDados.getObjectMapper().readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isEmpty()) {
                System.out.println("Não foi possível encontrar o livro buscado.");
                return;
            }
            
            // Conversão para objetos (LivroDTO)
            List<LivroDTO> livrosDTO = converteDados.getObjectMapper()
                    .readerForListOf(LivroDTO.class)
                    .readValue(resultsNode);

            // Remoção de possíveis valores duplicados
            duppedValues(titulo, livrosDTO);

            // Salva os novos livros no banco de dados
            if (!livrosDTO.isEmpty()) {
                System.out.println("Salvando novos livros encontrados...");
                List<Livro> novosLivros = livrosDTO.stream().map(Livro::new).collect(Collectors.toList());
                salvarLivros(novosLivros);
                System.out.println("Livros salvos com sucesso!");
            } else {
                System.out.println("Os livros já estão registrados no banco de dados!");
            }

            // Mostra livros encontrados
            if (!livrosDTO.isEmpty()) {
                System.out.println("Livros encontrados:");
                Set<String> titulosExibidos = new HashSet<>();
                for (LivroDTO livro : livrosDTO) {
                    if (!titulosExibidos.contains(livro.titulo())) {
                        System.out.println(livro);
                        titulosExibidos.add(livro.titulo());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livros: " + e.getMessage());
        }
    }

    private void duppedValues(String titulo, List<LivroDTO> livrosDTO) {
        List<Livro> livrosExistentes = livroRepository.findByTitulo(titulo);
        if (!livrosExistentes.isEmpty()) {
            System.out.println("Removendo livros duplicados já existentes no banco de dados...");
            for (Livro livroExistente : livrosExistentes) {
                livrosDTO.removeIf(livroDTO -> livroExistente.getTitulo().equals(livroDTO.titulo()));
            }
        }
    }

    private void listaLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    private void listaAutoresRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum autor registrado.");
        } else {
            livros.stream()
                    .map(Livro::getAutor)
                    .distinct()
                    .forEach(autor -> System.out.println(autor.getAutor()));
        }
    }

    private void listaAutoresVivos() {
        System.out.println("Digite um ano: ");
        int ano = entrada.nextInt();
        entrada.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresVivos(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {
            System.out.println("Lista de autores vivos no ano de " + ano + ":\n");

            autores.forEach(autor -> {
                if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())){
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
                }
            });
        }
    }

    private void listaAutoresVivosRefinado() {
        System.out.println("Digite o ano: ");
        int ano = entrada.nextInt();
        entrada.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresVivosRefinado(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {
            System.out.println("Lista de autores nascidos no ano de " + ano + ":\n");

            autores.forEach(autor -> {
                if(Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())){
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");

                }
            });
        }
    }

    private void listaAutoresPorAnoDeMorte() {
        System.out.println("Digite um ano: ");
        int ano = entrada.nextInt();
        entrada.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresPorAnoDeMorte(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {

            System.out.println("Lista de autores que morreram no ano de " + ano + ":\n");


            autores.forEach(autor -> {
                if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())){
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
                }
            });
        }
    }

    private void listaLivrosPorIdioma() {
        System.out.println("""
            Digite o idioma pretendido:
            Português (pt)
            Inglês (en)
            Espanhol (es)
            Francês (fr)
            Alemão (de)
           \s""");
        String idioma = entrada.nextLine();

        List<Livro> livros = livroRepository.findByIdioma(idioma);
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado no idioma escolhido!");
        } else {
            livros.forEach(livro -> {
                String titulo = livro.getTitulo();
                String autor = livro.getAutor().getAutor();
                String idiomaLivro = livro.getIdioma();

                System.out.println("Título: " + titulo);
                System.out.println("Autor: " + autor);
                System.out.println("Idioma: " + idiomaLivro);
                System.out.println("----------------------------------------");
            });
        }
    }
}