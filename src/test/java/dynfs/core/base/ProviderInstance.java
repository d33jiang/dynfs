package dynfs.core.base;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import dynfs.core.DynFileSystemProvider;

class ProviderInstance {

	DynFileSystemProvider provider;
	
	@Test
	void installation() {
		List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
		List<DynFileSystemProvider> dynProviders = providers.stream()
				.filter(fsp -> fsp instanceof DynFileSystemProvider)
				.map(dp -> (DynFileSystemProvider) dp)
				.collect(Collectors.toList());
		
		assertEquals(1, dynProviders.size());
		provider = dynProviders.get(0);
	}
	
}
