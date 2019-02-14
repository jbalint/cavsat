package com.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beans.Atom;
import com.beans.DenialConstraint;
import com.beans.Expression;
import com.beans.Query;
import com.beans.Relation;
import com.beans.Schema;

public class ProblemParser {

	public Query parseQueryFromFile(String filePath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String currentLine;
			Query query = new Query();
			int flag = 0;
			while ((currentLine = br.readLine()) != null) {
				if (currentLine.isEmpty()) {
					// Skip line
					continue;
				} else if (currentLine.equals("q")) {
					flag = 1;
					continue;
				} else if (currentLine.equals("free")) {
					flag = 2;
					continue;
				} else if (currentLine.equals("keys")) {
					flag = 3;
					continue;
				}

				if (flag == 1) {
					// Parse atoms
					String[] parts = currentLine.split("\t");
					Atom atom = new Atom(parts[0]);
					for (String var : parts[1].replaceAll(" ", "").split(",")) {
						// if (!isConstant(var))
						atom.addVar(var);
						atom.setAtomIndex(query.getAtomsCountByName(parts[0]) + 1);
					}
					query.addAtom(atom);
				} else if (flag == 2) {
					// Parse free variables
					String[] parts = currentLine.replaceAll(" ", "").split(",");
					for (String var : parts) {
						// if (!query.getFreeVars().contains(var) && !isConstant(var))
						query.getFreeVars().add(var);
					}
				} else if (flag == 3) {
					// Parse key and nonkey variables in the query
					String[] parts = currentLine.split("\t");
					Atom atom = query.getAtomByName(parts[0]);
					for (String var : parts[1].replaceAll(" ", "").split(",")) {
						// if (!atom.getKeyVars().contains(var) && !isConstant(var))
						atom.addKeyVar(var);
					}
					for (String var : atom.getVars()) {
						if (!atom.getKeyVars().contains(var))
							atom.getNonKeyVars().add(var);
					}
				}
			}
			return query;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public List<Query> parseUCQ(String filePath) {
		BufferedReader br = null;
		List<Query> uCQ = new ArrayList<Query>();
		try {
			br = new BufferedReader(new FileReader(filePath));
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				Query query = new Query();
				String parts[] = currentLine.split(":");
				String head = parts[0];
				String body = parts[1];
				for (String s : head.replaceAll("\\(", "").replaceAll("\\)", "").split(",")) {
					if (!query.getFreeVars().contains(s))
						query.getFreeVars().add(s);
				}
				for (String s : body.split(";")) {
					query.addAtom(parseAtom(s));
				}
				uCQ.add(query);
			}
			br.close();
			return uCQ;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Schema parseSchema(String filePath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String currentLine;
			Schema schema = new Schema();
			Set<Relation> relations = new HashSet<Relation>();
			int lineStatus = 0, constraintID = 0;
			while ((currentLine = br.readLine()) != null) {
				if (currentLine.isEmpty()) {
					continue;
				} else if (currentLine.equals("s")) {
					lineStatus = 1;
					continue;
				} else if (currentLine.equals("dc")) {
					lineStatus = 2;
					continue;
				} else if (currentLine.equals("keys")) {
					lineStatus = 3;
					continue;
				}
				String[] parts = null;
				Relation relation = null;
				DenialConstraint dc = null;
				switch (lineStatus) {
				case 1:
					parts = currentLine.split("\t");
					relation = new Relation(parts[0]);
					for (String attributeName : parts[1].split(",")) {
						relation.addAttribute(attributeName);
					}
					relations.add(relation);
					break;
				case 2:
					dc = new DenialConstraint(constraintID++);
					parts = currentLine.split(";");
					for (String s : parts) {
						if (Arrays.stream(Constants.ops).parallel().anyMatch(s::contains))
							dc.getExpressions().add(parseExpression(s));
						else
							dc.getAtoms().add(parseAtom(s));
					}
					schema.getConstraints().add(dc);
					break;
				case 3:
					parts = currentLine.split("\t");
					for (Relation r : relations) {
						if (r.getName().equals(parts[0])) {
							relation = r;
							break;
						}
					}
					for (String attributeIndex : parts[1].split(",")) {
						relation.addKeyAttribute(Integer.parseInt(attributeIndex));
					}
					break;
				default:
					break;
				}
			}
			schema.setRelations(relations);
			return schema;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	private Expression parseExpression(String expStr) {
		String[] parts;
		for (String op : Constants.ops) {
			if (expStr.contains(op)) {
				parts = expStr.split(op);
				Expression expObj = new Expression();
				expObj.setVar1(parts[0]);
				expObj.setVar2(parts[1]);
				expObj.setOp(op);
				return expObj;
			}
		}
		return null;
	}

	private VarProperties checkVarProperties(String var) {
		boolean isKey = false, isConstant = false;
		String v = var;
		if (v.startsWith("{") && v.endsWith("}")) {
			v = v.replaceAll("\\{", "").replaceAll("\\}", "");
			isKey = true;
		}
		if (v.startsWith("'") && v.endsWith("'")) {
			v = v.replaceAll("'", "");
			isConstant = true;
		}
		return new VarProperties(v, isKey, isConstant);
	}

	private Atom parseAtom(String atomStr) {
		String parts[] = atomStr.split("\\(");
		Atom atom = new Atom(parts[0]);
		parts = parts[1].replaceAll("\\)", "").split(",");
		for (String s : parts) {
			VarProperties p = checkVarProperties(s);
			if (p.isKey) {
				atom.addKeyVar(p.var);
			} else {
				atom.addNonKeyVar(p.var);
			}
			if (p.isConstant)
				atom.getConstants().add(p.var);
			atom.addVar(p.var);
		}
		return atom;
	}

	private class VarProperties {
		private String var;
		private boolean isKey;
		private boolean isConstant;

		public VarProperties(String var, boolean isKey, boolean isConstant) {
			super();
			this.var = var;
			this.isKey = isKey;
			this.isConstant = isConstant;
		}
	}

}