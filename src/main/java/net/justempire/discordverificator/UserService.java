package net.justempire.discordverificator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import net.justempire.discordverificator.exceptions.MinecraftUsernameAlreadyLinkedException;
import net.justempire.discordverificator.exceptions.NoVerificationsFoundException;
import net.justempire.discordverificator.exceptions.NotFoundException;
import net.justempire.discordverificator.exceptions.UserNotFoundException;
import net.justempire.discordverificator.models.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// Manipulates users in JSON
public class UserService {
    private List<User> userList = new ArrayList<>();
    private String pathToJson;

    public UserService(String pathToJson) {
        setUp(pathToJson);
    }

    public User getByUserId(String userId) throws UserNotFoundException {
        for (User user : userList) {
            if (user.getDiscordId().equalsIgnoreCase(userId)) return user;
        }

        throw new UserNotFoundException();
    }

    public User getByMinecraftUsername(String minecraftUsername) throws UserNotFoundException {
        for (User user : userList) {
            for (String username : user.linkedMinecraftUsernames) {
                if (minecraftUsername.equalsIgnoreCase(username))
                    return user;
            }
        }

        throw new UserNotFoundException();
    }

    public void updateLatestVerificationTimeFromIp(String userId, String ip) throws UserNotFoundException {
        User user = getByUserId(userId);
        user.updateLatestVerificationTimeFromIp(ip);
        saveUsers();
    }

    public Date getLatestVerificationTimeFromIp(String userId, String ip) throws UserNotFoundException, NoVerificationsFoundException {
        User user = getByUserId(userId);
        return user.getLatestVerificationTimeFromIp(ip);
    }

    public boolean isIpIgnored(String userId, String ip) throws UserNotFoundException {
        User user = getByUserId(userId);
        return user.isIpIgnored(ip);
    }

    public void ignoreIp(String userId, String ipToIgnore, Date ignoreUntil) throws UserNotFoundException {
        User user = getByUserId(userId);
        user.ignoreIp(ipToIgnore, ignoreUntil);
        saveUsers();
    }

    public void unIgnoreIp(String userId, String ipToUnIgnore) throws UserNotFoundException {
        User user = getByUserId(userId);
        user.unIgnoreIp(ipToUnIgnore);
        saveUsers();
    }

    public void updateIp(String discordId, String newIp) throws UserNotFoundException {
        for (User user : userList) {
            if (!user.getDiscordId().equals(discordId)) continue;

            user.setCurrentAllowedIp(newIp);
            saveUsers();
            return;
        }

        throw new UserNotFoundException();
    }

    public void linkUser(String discordId, String minecraftUsername) throws MinecraftUsernameAlreadyLinkedException {
        for (User user : userList) {
            if (user.isMinecraftUsernameLinked(minecraftUsername)) throw new MinecraftUsernameAlreadyLinkedException();
            if (!user.getDiscordId().equals(discordId)) continue;

            // If user found, link his minecraft account
            user.linkMinecraftUsername(minecraftUsername);

            saveUsers();
            return;
        }

        // If user not found, create one
        User user = new User(discordId, Arrays.asList(minecraftUsername), new ArrayList<>(), new ArrayList<>(), "");
        addUser(user);
    }

    public void unlinkUser(String minecraftUsername) throws NotFoundException {
        for (User user : userList) {
            if (!user.isMinecraftUsernameLinked(minecraftUsername)) continue;

            try { user.unlinkMinecraftUsername(minecraftUsername); }
            catch (Exception ignore) { }

            saveUsers();
            return;
        }

        throw new NotFoundException();
    }

    private void addUser(User userToAdd) {
        userList.add(userToAdd);
        saveUsers();
    }

    public void reload() {
        setUp(pathToJson);
    }

    public void onShutDown() {
        saveUsers();
    }

    private void setUp(String pathToJson) {
        try {
            this.pathToJson = pathToJson;
            loadUsers();
        }
        catch (MismatchedInputException e) {
            userList = new ArrayList<>();
            saveUsers();
            try { loadUsers(); }
            catch (IOException ex) { throw new RuntimeException(); }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadUsers() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        File source = new File(pathToJson);

        try { userList = objectMapper.readValue(source, new TypeReference<List<User>>() {}); }
        catch (FileNotFoundException e) {
            if (source.createNewFile()) loadUsers();
        }
    }

    private void saveUsers() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        try { objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathToJson), userList); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}
