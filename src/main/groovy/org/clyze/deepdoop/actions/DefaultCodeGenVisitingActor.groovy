package org.clyze.deepdoop.actions

import java.nio.file.Files
import java.nio.file.Paths
import org.clyze.deepdoop.system.Result

class DefaultCodeGenVisitingActor extends PostOrderVisitor<String> implements IActor<String>, TDummyActor<String> {

	File outDir

	AtomCollectingActor acActor = new AtomCollectingActor()
	List<Result> results        = []

	DefaultCodeGenVisitingActor(File outDir) {
		// Implemented this way, because Java doesn't allow usage of "this"
		// keyword before all implicit/explicit calls to super/this have
		// returned
		super(null)
		actor = this
		this.outDir = outDir
	}

	protected def createUniqueFile(String prefix, String suffix) {
		Files.createTempFile(Paths.get(outDir.name), prefix, suffix).toFile()
	}

	protected void write(File file, String data) { file << data << "\n" }
	protected void write(File file, List<String> data) { data.each { write file, it } }
}