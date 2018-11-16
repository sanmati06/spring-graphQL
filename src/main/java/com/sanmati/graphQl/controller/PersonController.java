package com.sanmati.graphQl.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.annotation.PostConstruct;


import com.sanmati.graphQl.dao.PersonRepository;
import com.sanmati.graphQl.entity.Person;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;

@RestController
public class PersonController {

	@Autowired
	private PersonRepository repository;

	@Value("classpath:person.graphqls")
	private Resource schemaResource;


	private GraphQL graphQL;

	@PostConstruct
	public void loadSchema() throws IOException {
		File schemaFile = schemaResource.getFile();
		TypeDefinitionRegistry registry = new SchemaParser().parse(schemaFile);
		RuntimeWiring wiring = buildWiring();
		GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
		graphQL = GraphQL.newGraphQL(schema).build();
	}
	private RuntimeWiring buildWiring(){
		DataFetcher<List<Person>> getAllPersonFetcher=new DataFetcher<List<Person>>() {
			@Override
			public List<Person> get(DataFetchingEnvironment dataFetchingEnvironment) {
				return (List<Person>) repository.findAll();
			}
		};
		DataFetcher<Person> findPersonFetcher =new DataFetcher<Person>() {
			@Override
			public Person get(DataFetchingEnvironment dataFetchingEnvironment) {
				return repository.findByEmail(dataFetchingEnvironment.getArgument("email"));
			}
		};

		DataFetcher<String> deletePersonFetcher=new DataFetcher<String>(){
			@Override
			public String get(DataFetchingEnvironment dataFetchingEnvironment) {
				try {
					repository.deleteById(dataFetchingEnvironment.getArgument("id"));
				}catch (EmptyResultDataAccessException e){
					return "Record not found";
				}
				return "Record deleted";
			}
		};
		return RuntimeWiring.newRuntimeWiring().type("Query", new UnaryOperator<TypeRuntimeWiring.Builder>() {
			@Override
			public TypeRuntimeWiring.Builder apply(TypeRuntimeWiring.Builder builder) {
				return builder.dataFetcher("getAllPerson", getAllPersonFetcher).dataFetcher("findPerson", findPersonFetcher).dataFetcher("deletePerson",deletePersonFetcher);
			}
		}).build();
	}

	@PostMapping(value = "/addPerson")
	public String addPerson(@RequestBody List<Person> persons) {
		repository.saveAll(persons);
		return "record inserted " + persons.size();
	}

	@GetMapping("/findAllPerson")
	public List<Person> getPersons() {
		return (List<Person>) repository.findAll();
	}

	@PostMapping("/getAll")
	public ResponseEntity<Object> getAll(@RequestBody String query) {
		ExecutionResult result = graphQL.execute(query);
		return new ResponseEntity<Object>(result, HttpStatus.OK);
	}

	@PostMapping("/getPersonByEmail")
	public ResponseEntity<Object> getPersonByEmail(@RequestBody String query) {
		ExecutionResult result = graphQL.execute(query);
		return new ResponseEntity<Object>(result, HttpStatus.OK);
	}
	@PostMapping("/deletePersonById")
	public ResponseEntity<Object> deletePerson(@RequestBody String query){
		ExecutionResult result=graphQL.execute(query);
		return new ResponseEntity<Object>(result, HttpStatus.OK);
	}
}
