package org.neo4j.backup;

import org.jboss.netty.buffer.ChannelBuffer;
import org.neo4j.com.Client;
import org.neo4j.com.MasterCaller;
import org.neo4j.com.ObjectSerializer;
import org.neo4j.com.Protocol;
import org.neo4j.com.RequestType;
import org.neo4j.com.Response;
import org.neo4j.com.SlaveContext;
import org.neo4j.com.StoreWriter;
import org.neo4j.com.ToNetworkStoreWriter;

class BackupClient extends Client<TheBackupInterface> implements TheBackupInterface
{
    public BackupClient( String hostNameOrIp, int port, String storeDir )
    {
        super( hostNameOrIp, port, storeDir );
    }
    
    public Response<Void> fullBackup( StoreWriter storeWriter )
    {
        return sendRequest( BackupRequestType.FULL_BACKUP, SlaveContext.EMPTY,
                Protocol.EMPTY_SERIALIZER, new Protocol.FileStreamsDeserializer( storeWriter ) );
    }
    
    public Response<Void> incrementalBackup( SlaveContext context )
    {
        return sendRequest( BackupRequestType.INCREMENTAL_BACKUP, context,
                Protocol.EMPTY_SERIALIZER, Protocol.VOID_DESERIALIZER );
    }
    
    public static enum BackupRequestType implements RequestType<TheBackupInterface>
    {
        FULL_BACKUP( new MasterCaller<TheBackupInterface, Void>()
        {
            public Response<Void> callMaster( TheBackupInterface master, SlaveContext context,
                    ChannelBuffer input, ChannelBuffer target )
            {
                return master.fullBackup( new ToNetworkStoreWriter( target ) );
            }
        }, Protocol.VOID_SERIALIZER ),
        INCREMENTAL_BACKUP( new MasterCaller<TheBackupInterface, Void>()
        {
            public Response<Void> callMaster( TheBackupInterface master, SlaveContext context,
                    ChannelBuffer input, ChannelBuffer target )
            {
                return master.incrementalBackup( context );
            }
        }, Protocol.VOID_SERIALIZER )
        
        ;
        @SuppressWarnings( "rawtypes" )
        private final MasterCaller masterCaller;
        @SuppressWarnings( "rawtypes" )
        private final ObjectSerializer serializer;
        
        @SuppressWarnings( "rawtypes" )
        private BackupRequestType( MasterCaller masterCaller, ObjectSerializer serializer )
        {
            this.masterCaller = masterCaller;
            this.serializer = serializer;
        }

        @SuppressWarnings( "rawtypes" )
        public MasterCaller getMasterCaller()
        {
            return masterCaller;
        }

        @SuppressWarnings( "rawtypes" )
        public ObjectSerializer getObjectSerializer()
        {
            return serializer;
        }

        public byte id()
        {
            return (byte) ordinal();
        }
    }
}
