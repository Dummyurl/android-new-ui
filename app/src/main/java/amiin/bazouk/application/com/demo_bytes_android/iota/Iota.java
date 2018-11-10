package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesAndFormatResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.IotaAPIUtils;
import jota.utils.StopWatch;

public class Iota {
    private IotaAPI iotaAPI;
    private String seed;

    public int minWeightMagnitude = 14;
    public int depth = 3;
    public int security = 2;

    public Iota(String protocol, String host, String port, String seed)
    {
        iotaAPI = new IotaAPI.Builder()
                .protocol(protocol)
                .host(host)
                .port(port)
                .build();
        this.seed = seed;
    }

    public String getLatestMilestone() throws ArgumentException {
        GetNodeInfoResponse nodeInfo = iotaAPI.getNodeInfo();
        String latestMilestoneHash = nodeInfo.getLatestMilestone();
        // System.out.println("\n NodeInfo: Latest Milestone Index: " + latestMilestoneHash);

        return latestMilestoneHash;
    }

    public List<String> makeTx(String addressTo, long amountIni) throws ArgumentException {
        boolean validateInputs = true;
        List<Input> inputs = new ArrayList<Input>();
        List<Transaction> tips = new ArrayList<Transaction>();

        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(addressTo, amountIni));

        String remainderAddress = this.getCurrentAddress();

        // bundle prep for all transfers
        System.out.println("before prepareTransfers: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        List<String> trytesBundle = iotaAPI.prepareTransfers(seed, security, transfers, remainderAddress, inputs, tips, validateInputs);
        System.out.println("after prepareTransfers: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );

        String[] trytes = trytesBundle.toArray(new String[0]);
        String reference = getLatestMilestone();

        System.out.println("before sendTrytes: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        List<Transaction> transactions = iotaAPI.sendTrytes(trytes, depth, minWeightMagnitude, reference);
        System.out.println("after sendTrytes: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        System.out.println("\n transactions: " + transactions);


        List<String> tails = new ArrayList<String>();
        for (Transaction t : transactions) {
            tails.add(t.getHash());
        }
        return tails;
    }

    public Boolean verifyTx(List<String> tails) {
        return true;
    }

    public String getCurrentAddress() throws ArgumentException {
        boolean checksum = true;

        GetNewAddressResponse getNewAddressResponse = iotaAPI.getNextAvailableAddress(seed, security, checksum);

        return getNewAddressResponse.getAddresses().get(0);
    }

    public long getBalance() throws ArgumentException {

        String currentAddress = this.getCurrentAddress();
        List<String> tips = new ArrayList<String>();
        long threshold = 0;
        int start = 0; //currentAddressIndex
        StopWatch stopWatch = new StopWatch();

        GetBalancesAndFormatResponse res = iotaAPI.getBalanceAndFormat(
                Arrays.asList(currentAddress),
                tips,
                threshold,
                start,
                stopWatch,
                security);
        return res.getTotalBalance();
    }

    public Integer getAvailableAddressIndex(Integer lastKnownAddressIndex) throws ArgumentException {
        int i = lastKnownAddressIndex == null ? -1 : lastKnownAddressIndex;

        while (true) {
            i++;
            String newAddress = IotaAPIUtils.newAddress(seed, 2, i, false, null);
            System.out.println(i + "   " + newAddress);
            if (iotaAPI.findTransactionsByAddresses(new String[]{newAddress}).getHashes().length == 0) {
                return i;
            }
        }
    }

    private String getAddress(int index) throws ArgumentException {
        return IotaAPIUtils.newAddress(seed, 2, index, false, null);
    }
}