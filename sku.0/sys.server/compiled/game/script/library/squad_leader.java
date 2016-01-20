package script.library;

import script.*;
import script.base_class.*;
import script.combat_engine.*;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import script.base_script;

import script.library.chat;
import script.library.skill;
import script.library.prose;

public class squad_leader extends script.base_script
{
    public squad_leader()
    {
    }
    public static final int MAX_SKILL_VALUE = 20;
    public static final int MIN_SKILL_VALUE = 0;
    public static final int MAX_GROUP_SIZE = 20;
    public static final String SKILL_NAME = "outdoors_squadleader";
    public static final string_id SID_SYS_SL = new string_id("squad_leader", "squad_leader_command");
    public static final string_id SID_NOT_IN_A_GROUP = new string_id("squad_leader", "not_in_a_group");
    public static final string_id SID_MESSAGE_PARAMETERS = new string_id("squad_leader", "message_parameters");
    public static void sendSquadLeaderCommand(obj_id squadLeader, String command) throws InterruptedException
    {
        obj_id groupId = getGroupObject(squadLeader);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        if (command.equals("a") || command.equals("c"))
        {
            sendSystemMessage(squadLeader, SID_MESSAGE_PARAMETERS);
            return;
        }
        int i = 0;
        int textlen = command.length();
        while (textlen > i && '@' == command.charAt(i))
        {
            ++i;
        }
        command = command.substring(i, textlen);
        if (command == null || command.equals(""))
        {
            sendSystemMessage(squadLeader, SID_MESSAGE_PARAMETERS);
            return;
        }
        if (groupMembers != null)
        {
            for (int j = 0; j < groupMembers.length; ++j)
            {
                displayCommand(squadLeader, groupMembers[j], command);
            }
        }
        else 
        {
            sendSystemMessage(squadLeader, SID_NOT_IN_A_GROUP);
            return;
        }
    }
    public static boolean sendSquadWaypoint(obj_id officer, location wayLoc) throws InterruptedException
    {
        if (!isIdValid(officer) || !exists(officer))
        {
            return false;
        }
        obj_id groupId = getGroupObject(officer);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        dictionary dict = new dictionary();
        dict.put("wayLoc", wayLoc);
        if (groupMembers != null)
        {
            for (int i = 0; i < groupMembers.length; ++i)
            {
                messageTo(groupMembers[i], "createOfficerGroupWaypoint", dict, 0, false);
            }
        }
        else 
        {
            sendSystemMessage(officer, SID_NOT_IN_A_GROUP);
            return false;
        }
        return true;
    }
    public static void displayCommand(obj_id squadLeader, obj_id groupMember, String command) throws InterruptedException
    {
        if (groupMember != null)
        {
            prose_package pp = prose.getPackage(SID_SYS_SL, getName(squadLeader), command);
            sendSystemMessageProse(groupMember, pp);
            return;
        }
        else 
        {
            return;
        }
    }
    public static boolean hasSkillsToSendCommand(obj_id squadLeader, int groupSize) throws InterruptedException
    {
        String skillPointScale = MIN_SKILL_VALUE + ".." + MAX_SKILL_VALUE;
        int percent = skill.check(squadLeader, SKILL_NAME, skillPointScale);
        int groupPercent = groupSize / MAX_GROUP_SIZE;
        if (percent >= groupPercent)
        {
            return true;
        }
        return false;
    }
    public static obj_id[] getSquadTargets(obj_id self, String requiredSkill) throws InterruptedException
    {
        if (!hasSkill(self, requiredSkill))
        {
            showFlyTextPrivate(self, self, new string_id("combat_effects", "action_failed"), 1.5f, colors.WHITE);
            combat.sendCombatSpamMessage(self, new string_id("cbt_spam", "squad_leader_no_skill"));
            return null;
        }
        obj_id groupId = getGroupObject(self);
        if (groupId == null)
        {
            showFlyTextPrivate(self, self, new string_id("combat_effects", "action_failed"), 1.5f, colors.WHITE);
            combat.sendCombatSpamMessage(self, new string_id("cbt_spam", "squad_leader_no_group"));
            return null;
        }
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        obj_id groupLeaderId = getGroupLeaderId(groupId);
        if ((self != groupLeaderId) || (groupMembers == null))
        {
            showFlyTextPrivate(self, self, new string_id("combat_effects", "action_failed"), 1.5f, colors.WHITE);
            combat.sendCombatSpamMessage(self, new string_id("cbt_spam", "squad_leader_not_leader"));
            return null;
        }
        return groupMembers;
    }
    public static boolean setSquadCommandText(String command, String params) throws InterruptedException
    {
        obj_id self = getSelf();
        String varName = "combat." + command + "String";
        int intLength = params.length();
        if (intLength > 64)
        {
            sendSystemMessage(self, new string_id("error_message", "string_too_long"));
            return false;
        }
        String strString = params.substring(0, intLength);
        setObjVar(self, varName, strString);
        return true;
    }
    public static String getSquadCommandText(String command) throws InterruptedException
    {
        obj_id self = getSelf();
        String varName = "combat." + command + "String";
        String text = getStringObjVar(self, varName);
        return text;
    }
    public static void barkSquadCommand(String command, String defaultText) throws InterruptedException
    {
        obj_id self = getSelf();
        String text = getSquadCommandText(command);
        if ((text == null) || text.equals(""))
        {
            text = defaultText;
        }
    }
    public static obj_id[] getValidGroupMembers(obj_id self) throws InterruptedException
    {
        obj_id gid = getGroupObject(self);
        if (!group.isGroupObject(gid))
        {
            return null;
        }
        obj_id[] groupMembers = getGroupMemberIds(gid);
        Vector validMembers = new Vector();
        validMembers.setSize(0);
        for (int i = 0; i < groupMembers.length; i++)
        {
            if (!(isIdValid(groupMembers[i]) && exists(groupMembers[i]) && isPlayer(groupMembers[i]) && getDistance(self, groupMembers[i]) < 64.0f))
            {
                continue;
            }
            validMembers = utils.addElement(validMembers, groupMembers[i]);
        }
        obj_id[] _validMembers = new obj_id[0];
        if (validMembers != null)
        {
            _validMembers = new obj_id[validMembers.size()];
            validMembers.toArray(_validMembers);
        }
        return _validMembers;
    }
    public static void validateRallyPoint(obj_id player) throws InterruptedException
    {
        if (!hasObjVar(player, "sl.rallyPoint"))
        {
            return;
        }
        obj_id gid = getGroupObject(player);
        obj_id rallyGroup = getObjIdObjVar(player, "sl.rallyPoint.group");
        if (rallyGroup != gid)
        {
            clearRallyPoint(player);
        }
    }
    public static void clearRallyPoint(obj_id player) throws InterruptedException
    {
        if (!hasObjVar(player, "sl.rallyPoint"))
        {
            return;
        }
        obj_id rallyPoint = getObjIdObjVar(player, "sl.rallyPoint.id");
        destroyWaypointInDatapad(rallyPoint, player);
        removeObjVar(player, "sl.rallyPoint");
    }
    public static int getCalledShotAccuracyBonus(obj_id attacker, obj_id defender) throws InterruptedException
    {
        obj_id gid = getGroupObject(attacker);
        if (!group.isGroupObject(gid))
        {
            return 0;
        }
        if (!utils.hasScriptVar(defender, "calledShot." + gid))
        {
            return 0;
        }
        return utils.getIntScriptVar(defender, "calledShot." + gid + ".accuracy");
    }
    public static int getCalledShotDamageBonus(obj_id attacker, obj_id defender) throws InterruptedException
    {
        obj_id gid = getGroupObject(attacker);
        if (!group.isGroupObject(gid))
        {
            return 0;
        }
        if (!utils.hasScriptVar(defender, "calledShot." + gid))
        {
            return 0;
        }
        return utils.getIntScriptVar(defender, "calledShot." + gid + ".damage");
    }
    public static void clearCalledShotEffect(obj_id attacker, obj_id defender) throws InterruptedException
    {
        obj_id gid = getGroupObject(attacker);
        if (!group.isGroupObject(gid))
        {
            return;
        }
        if (!utils.hasScriptVar(defender, "calledShot." + gid))
        {
            return;
        }
        utils.removeScriptVarTree(defender, "calledShot." + gid);
        stopClientEffectObjByLabel(new obj_id[]
        {
            defender
        }, defender, "calledShot." + gid);
    }
    public static float getLeadershipMod(obj_id player) throws InterruptedException
    {
        int skillMod = getEnhancedSkillStatisticModifier(player, "leadership");
        if (skillMod > 200)
        {
            skillMod = 200;
        }
        float modifier = (skillMod + 75f) / 200f;
        return modifier;
    }
}